package manage.control;

import manage.format.EngineBlockFormatter;
import manage.model.MetaData;
import manage.push.Delta;
import manage.push.PrePostComparator;
import manage.repository.MetaDataRepository;
import manage.web.PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
@RestController
public class DatabaseController {

    private RestTemplate restTemplate;
    private String pushUri;

    private RestTemplate oidcRestTemplate;
    private String oidcPushUri;
    private boolean oidcEnabled;

    private boolean excludeEduGainImported;
    private boolean excludeOidcRP;

    private MetaDataRepository metaDataRepository;
    private JdbcTemplate ebJdbcTemplate;
    private Environment environment;

    private PrePostComparator prePostComparator = new PrePostComparator();

    @Autowired
    DatabaseController(MetaDataRepository metaDataRepository,
                       DataSource ebDataSource,
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

        this.ebJdbcTemplate = new JdbcTemplate(ebDataSource);
        this.environment = environment;
    }

    public ResponseEntity<Map> doPush() {
        if (environment.acceptsProfiles("dev")) {
            return new ResponseEntity<>(Collections.singletonMap("status", 200), HttpStatus.OK);
        }

        List<Map<String, Object>> preProvidersData = ebJdbcTemplate.queryForList("SELECT * FROM " +
                "sso_provider_roles_eb5 ORDER BY id ASC");

        Map<String, Map<String, Map<String, Object>>> json = this.pushPreview();

        ResponseEntity<String> response = this.restTemplate.postForEntity(pushUri, json, String.class);
        HttpStatus statusCode = response.getStatusCode();

        List<Map<String, Object>> postProvidersData = ebJdbcTemplate.queryForList("SELECT * FROM " +
                "sso_provider_roles_eb5 ORDER BY id ASC");
        Set<Delta> deltas = prePostComparator.compare(preProvidersData, postProvidersData);

        List<String> knownDeltas = Arrays.asList("id", "name_id_formats");
        List<Delta> realDeltas = deltas.stream().filter(delta -> !knownDeltas.contains(delta.getAttribute())).collect
                (toList());
        realDeltas.sort(Comparator.comparing(Delta::getEntityId));

        Map<String, Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("response", response);
        result.put("deltas", realDeltas);

        // Now push all oidc_rp metadata to OIDC proxy
        if (!environment.acceptsProfiles("dev") && oidcEnabled) {
            List<MetaData> oidcClients = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "oidc10_rp");
            this.oidcRestTemplate.postForEntity(oidcPushUri, oidcClients, Void.class);
            result.put("oidc", true);
        }

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

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
            List<MetaData> oidcClients = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "oidc10_rp");
            Map<String, Map<String, Object>> oidcClientsToPush = oidcClients.stream()
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
        CloseableHttpClient httpClient = httpClientBuilder.build();
        return new PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory(httpClient, pushUri);
    }
}
