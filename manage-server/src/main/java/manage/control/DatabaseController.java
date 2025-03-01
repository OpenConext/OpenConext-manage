package manage.control;

import manage.format.EngineBlockFormatter;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.PushOptions;
import manage.model.Scope;
import manage.policies.PdpPolicyDefinition;
import manage.repository.MetaDataRepository;
import manage.web.HttpHostProvider;
import manage.web.PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory;
import org.apache.hc.client5.http.auth.AuthScope;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.TimeValue;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
@RestController
@SuppressWarnings("unchecked")
public class DatabaseController {

    private final RestTemplate restTemplate;
    private final String pushUri;

    private final RestTemplate oidcRestTemplate;
    private final String oidcPushUri;
    private final boolean oidcEnabled;

    private final boolean excludeEduGainImported;
    private final boolean excludeOidcRP;
    private final boolean excludeSRAM;

    private final MetaDataRepository metaDataRepository;

    private final Environment environment;
    private final String pdpPushUri;
    private final RestTemplate pdpRestTemplate;
    private final boolean pdpEnabled;

    @Autowired
    DatabaseController(MetaDataRepository metaDataRepository,
                       @Value("${push.eb.url}") String pushUri,
                       @Value("${push.eb.user}") String user,
                       @Value("${push.eb.password}") String password,
                       @Value("${push.eb.exclude_edugain_imports}") boolean excludeEduGainImported,
                       @Value("${push.eb.exclude_oidc_rp}") boolean excludeOidcRP,
                       @Value("${push.eb.exclude_sram}") boolean excludeSRAM,
                       @Value("${push.oidc.url}") String oidcPushUri,
                       @Value("${push.oidc.user}") String oidcUser,
                       @Value("${push.oidc.password}") String oidcPassword,
                       @Value("${push.pdp.url}") String pdpPushUri,
                       @Value("${push.pdp.user}") String pdpUser,
                       @Value("${push.pdp.password}") String pdpPassword,
                       @Value("${push.pdp.enabled}") boolean pdpEnabled,
                       @Value("${push.oidc.enabled}") boolean oidcEnabled,
                       Environment environment) throws MalformedURLException, URISyntaxException {
        this.metaDataRepository = metaDataRepository;
        this.pushUri = pushUri;
        this.restTemplate = new RestTemplate(getRequestFactory(user, password, pushUri));
        this.excludeEduGainImported = excludeEduGainImported;
        this.excludeOidcRP = excludeOidcRP;
        this.excludeSRAM = excludeSRAM;

        this.oidcRestTemplate = oidcEnabled ? new RestTemplate(getRequestFactory(oidcUser, oidcPassword,oidcPushUri )) : null;
        this.oidcPushUri = oidcPushUri;
        this.oidcEnabled = oidcEnabled;

        this.pdpRestTemplate = pdpEnabled ? new RestTemplate(getRequestFactory(pdpUser, pdpPassword, pdpPushUri)) : null;
        this.pdpPushUri = pdpPushUri;
        this.pdpEnabled = pdpEnabled;

        this.environment = environment;
    }

    public ResponseEntity<Map> doPush(PushOptions pushOptions) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            return new ResponseEntity<>(Collections.singletonMap("status", "OK"), HttpStatus.OK);
        }
        Map<String, Object> result = new HashMap<>();
        if (pushOptions.isIncludePdP() && pdpEnabled) {
            List<PdpPolicyDefinition> policies = this.metaDataRepository
                    .findAllByType(EntityType.PDP.getType()).stream()
                    .map(PdpPolicyDefinition::new)
                    .filter(PdpPolicyDefinition::isActive)
                    .collect(toList());
            this.pdpRestTemplate.put(pdpPushUri, policies);
            result.put("status", "OK");
            result.put("pdp", true);
        }
        if (pushOptions.isIncludeEB()) {
            Map<String, Map<String, Map<String, Object>>> json = this.pushPreview();

            ResponseEntity<String> response = this.restTemplate.postForEntity(pushUri, json, String.class);

            result.put("status", "OK");
            result.put("response", response);
        }

        // Now push all oidc_rp metadata to OIDC proxy
        if (!environment.acceptsProfiles(Profiles.of("dev")) && oidcEnabled && pushOptions.isIncludeOIDC()) {
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
            if (!excludeSRAM) {
                List<MetaData> sramRelyingParties = metaDataRepository.getMongoTemplate()
                        .findAll(MetaData.class, EntityType.SRAM.getType())
                        .stream()
                        .filter(sramRP -> "oidc_rp".equals(sramRP.metaDataFields().get("connection_type")))
                        .toList();
                relyingParties.addAll(sramRelyingParties);
            }
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
            result.put("status", "OK");
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/pushPreview")
    public Map<String, Map<String, Map<String, Object>>> pushPreview() {
        EngineBlockFormatter formatter = new EngineBlockFormatter();

        List<MetaData> serviceProviders = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.SP.getType());
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

        List<MetaData> identityProviders = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.IDP.getType());

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
        if (!excludeSRAM) {
            List<MetaData> sramServices = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.SRAM.getType());
            Map<String, Map<String, Object>> sramServicesToProvidersToPush = sramServices.stream()
                    .collect(toMap(MetaData::getId, formatter::parseServiceProvider));
            serviceProvidersToPush.putAll(sramServicesToProvidersToPush);
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

    private ClientHttpRequestFactory getRequestFactory(String user, String password, String uri) throws MalformedURLException, URISyntaxException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().evictExpiredConnections()
                .evictIdleConnections(TimeValue.of(10L, TimeUnit.SECONDS));
        BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
        // @TODO: suitable replacement for AuthScope.ANY?
        basicCredentialsProvider.setCredentials(new AuthScope(null, -1), new UsernamePasswordCredentials(user, password.toCharArray()));
        httpClientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);

        Optional<HttpHost> optionalHttpHost = HttpHostProvider.resolveHttpHost(URI.create(uri).toURL());
        optionalHttpHost.ifPresent(httpHost -> httpClientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(httpHost)));

        CloseableHttpClient httpClient = httpClientBuilder.build();
        return new PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory(httpClient, uri);
    }
}
