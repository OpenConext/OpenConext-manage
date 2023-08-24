package manage.control;

import manage.format.EngineBlockFormatter;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.Scope;
import manage.repository.MetaDataRepository;
import manage.web.PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
@RestController
public class DatabaseController {

    private final RestTemplate restTemplate;
    private final String pushUri;

    private final RestTemplate oidcRestTemplate;
    private final String oidcPushUri;
    private final boolean oidcEnabled;

    private final boolean excludeEduGainImported;
    private final boolean excludeOidcRP;

    private final MetaDataRepository metaDataRepository;

    private final Environment environment;

    @Autowired
    DatabaseController(MetaDataRepository metaDataRepository,
                       @Value("${push.eb.url}") String pushUri,
                       @Value("${push.eb.user}") String user,
                       @Value("${push.eb.password}") String password,
                       @Value("${push.eb.exclude_edugain_imports}") boolean excludeEduGainImported,
                       @Value("${push.eb.exclude_oidc_rp}") boolean excludeOidcRP,
                       @Value("${push.oidc.url}") String oidcPushUri,
                       @Value("${push.oidc.user}") String oidcUser,
                       @Value("${push.oidc.password}") String oidcPassword,
                       @Value("${push.oidc.enabled}") boolean oidcEnabled,
                       Environment environment) throws MalformedURLException {
        this.metaDataRepository = metaDataRepository;
        this.pushUri = pushUri;
        this.restTemplate = new RestTemplate(getRequestFactory(user, password));
        this.excludeEduGainImported = excludeEduGainImported;
        this.excludeOidcRP = excludeOidcRP;

        this.oidcRestTemplate = new RestTemplate(getRequestFactory(oidcUser, oidcPassword));
        this.oidcPushUri = oidcPushUri;
        this.oidcEnabled = oidcEnabled;

        this.environment = environment;
    }

    public ResponseEntity<Map> doPush() {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            return new ResponseEntity<>(Collections.singletonMap("status", 200), HttpStatus.OK);
        }

        Map<String, Map<String, Map<String, Object>>> json = this.pushPreview();

        ResponseEntity<String> response = this.restTemplate.postForEntity(pushUri, json, String.class);
        HttpStatus statusCode = response.getStatusCode();

        Map<String, Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("response", response);

        // Now push all oidc_rp metadata to OIDC proxy
        if (!environment.acceptsProfiles(Profiles.of("dev")) && oidcEnabled) {
            List<MetaData> relyingParties = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.RP.getType());
            List<MetaData> resourceServers = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.RS.getType());
            List<Scope> scopes = metaDataRepository.getMongoTemplate().findAll(Scope.class);
            Map<String, Scope> scopesMapped = scopes.stream().collect(toMap(Scope::getName, scope -> scope));
            resourceServers.forEach(rs -> {
                //Once we want to get rid of this hack, but for now backward compatibility
                rs.setType(EntityType.RP.getType());
                Map<String, Object> metaDataFields = rs.metaDataFields();
                metaDataFields.put("isResourceServer", true);
                List<String> scopeList = (List<String>) metaDataFields.get("scopes");
                if (!CollectionUtils.isEmpty(scopeList)) {
                    List<Scope> transformedScope = scopeList.stream()
                            .map(scope -> scopesMapped.getOrDefault(scope, null))
                            .filter(Objects::nonNull)
                            .collect(toList());
                    metaDataFields.put("scopes", transformedScope);
                }
            });
            relyingParties.forEach(rp -> {
                //Once we want to get rid of this cleanup, but for now backward compatibility
                Map<String, Object> metaDataFields = rp.metaDataFields();
                metaDataFields.put("isResourceServer", false);
                metaDataFields.remove("scopes");
            });
            relyingParties.addAll(resourceServers);
            List<MetaData> filteredEntities = relyingParties.stream()
                    .filter(metaData -> !excludeFromPush(metaData.metaDataFields()))
                    .collect(toList());
            this.oidcRestTemplate.postForEntity(oidcPushUri, filteredEntities, Void.class);
            result.put("oidc", true);
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/client/playground/pushPreview")
    public Map<String, Map<String, Map<String, Object>>> pushPreview() {
        EngineBlockFormatter formatter = new EngineBlockFormatter();

        List<MetaData> serviceProviders = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_sp");
        Stream<MetaData> metaDataStream = excludeEduGainImported ?
                serviceProviders.stream()
                        .filter(metaData -> {
                            Map metaDataFields = metaData.metaDataFields();
                            boolean importedFromEdugain = Boolean.TRUE.equals(metaDataFields.get("coin:imported_from_edugain"));
                            boolean pushEnabled = Boolean.TRUE.equals(metaDataFields.get("coin:push_enabled"));
                            return !importedFromEdugain || pushEnabled;
                        }) : serviceProviders.stream();

        Map<String, Map<String, Object>> serviceProvidersToPush = metaDataStream
                .filter(metaData -> !excludeFromPush(metaData.metaDataFields()))
                .collect(toMap(MetaData::getId, formatter::parseServiceProvider));

        List<MetaData> identityProviders = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_idp");

        //Explicit only filter out 'null' objects in the disableConsent as generically filtering out 'nulls' can break things
        filterOutNullDisableConsentExplanations(identityProviders);

        Map<String, Map<String, Object>> identityProvidersToPush = identityProviders.stream()
                .filter(metaData -> !excludeFromPush(metaData.metaDataFields()))
                .collect(toMap(MetaData::getId, formatter::parseIdentityProvider));

        if (!excludeOidcRP) {
            List<MetaData> relyingParties = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.RP.getType());
            Map<String, Map<String, Object>> oidcClientsToPush = relyingParties.stream()
                    .filter(metaData -> !excludeFromPush(metaData.metaDataFields()))
                    .collect(toMap(MetaData::getId, formatter::parseOidcClient));

            serviceProvidersToPush.putAll(oidcClientsToPush);
        }

        serviceProvidersToPush.putAll(identityProvidersToPush);

        Map<String, Map<String, Map<String, Object>>> results = new HashMap<>();
        results.put("connections", serviceProvidersToPush);

        return results;
    }

    private boolean excludeFromPush(Map metaDataFields) {
        Object excludeFromPush = metaDataFields.getOrDefault("coin:exclude_from_push", false);
        if (excludeFromPush instanceof String) {
            return "1".equals(excludeFromPush);
        }
        return (boolean) excludeFromPush;
    }

    @SuppressWarnings("unchecked")
    public void filterOutNullDisableConsentExplanations(List<MetaData> identityProviders) {
        identityProviders.forEach(idp -> {
            Object disableConsentData = idp.getData().get("disableConsent");
            if (disableConsentData instanceof List) {
                List disableConsent = (List) disableConsentData;
                disableConsent.forEach(disableConsentEntry -> {
                    if (disableConsentEntry instanceof Map) {
                        Map<String, Object> disableConsentMap = (Map) disableConsentEntry;
                        disableConsentMap.entrySet().removeIf(entry -> entry.getValue() == null);
                    }
                });
            }
        });
    }

    private ClientHttpRequestFactory getRequestFactory(String user, String password) throws MalformedURLException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().evictExpiredConnections()
                .evictIdleConnections(10l, TimeUnit.SECONDS);
        BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
        basicCredentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(user, password));
        httpClientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);

        String proxyHost = System.getProperty("http.proxyHost");
        String proxyPortString = System.getProperty("http.proxyPort");
        int proxyPort = StringUtils.hasText(proxyPortString) ? Integer.parseInt(proxyPortString) : 8080;

        if (proxyHost != null) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            httpClientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(proxy));
        }

        CloseableHttpClient httpClient = httpClientBuilder.build();
        return new PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory(httpClient, pushUri);
    }
}
