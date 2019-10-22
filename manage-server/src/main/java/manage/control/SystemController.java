package manage.control;

import manage.api.APIUser;
import manage.conf.Features;
import manage.exception.EndpointNotAllowed;
import manage.format.EngineBlockFormatter;
import manage.hook.EntityIdReconcilerHook;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.OrphanMetaData;
import manage.push.Delta;
import manage.push.PrePostComparator;
import manage.repository.MetaDataRepository;
import manage.shibboleth.FederatedUser;
import manage.validations.MetaDataValidator;
import manage.web.PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RestController
@SuppressWarnings("unchecked")
public class SystemController {

    private static final Logger LOG = LoggerFactory.getLogger(SystemController.class);

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
    private MetaDataValidator metaDataValidator;

    private PrePostComparator prePostComparator = new PrePostComparator();

    @Autowired
    public SystemController(MetaDataRepository metaDataRepository,
                            MetaDataValidator metaDataValidator,
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
        this.metaDataValidator = metaDataValidator;
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

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/push")
    public ResponseEntity<Map> push(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.PUSH)) {
            throw new EndpointNotAllowed();
        }
        return doPush();
    }

    @PreAuthorize("hasRole('PUSH')")
    @GetMapping("/internal/push")
    public ResponseEntity<Map> pushInternal(APIUser apiUser) {
        LOG.info("Push initiated by {}", apiUser.getName());
        return doPush();
    }

    private ResponseEntity<Map> doPush() {
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

    @GetMapping("/client/playground/validate")
    public Map<String, Object> validate(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.VALIDATION)) {
            throw new EndpointNotAllowed();
        }
        return metaDataValidator.validateMigration();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping({"/client/playground/deleteOrphans"})
    public void deleteOrphans() {
        doDeleteOrphans();
    }

    @PreAuthorize("hasRole('SYSTEM')")
    @DeleteMapping({"/internal/playground/deleteOrphans"})
    public void deleteOrphansInternal() {
        doDeleteOrphans();
    }

    private void doDeleteOrphans() {
        List<OrphanMetaData> orphans = this.orphans();
        orphans.forEach(orphanMetaData -> {
            MetaData metaData = metaDataRepository.findById(orphanMetaData.getId(), orphanMetaData.getCollection());
            List<Map<String, Object>> entries =
                    (List<Map<String, Object>>) metaData.getData().get(orphanMetaData.getReferencedCollectionName());

            List<Map<String, Object>> newEntries = entries.stream().filter(entry -> !entry.get("name").equals
                    (orphanMetaData.getMissingEntityId())).collect(Collectors.toList());
            metaData.getData().put(orphanMetaData.getReferencedCollectionName(), newEntries);
            metaData.getData().put("revisionnote", "Removed reference to non-existent entityID");
            MetaData previous = metaDataRepository.findById(metaData.getId(), orphanMetaData.getCollection());
            previous.revision(UUID.randomUUID().toString());
            metaDataRepository.save(previous);
            metaData.promoteToLatest("System");
            metaDataRepository.update(metaData);

        });
    }

    @GetMapping({"/client/playground/orphans", "/internal/playground/orphans"})
    public List<OrphanMetaData> orphans() {
        return Stream.of(EntityType.values()).map(this::orphanMetaData)
                .flatMap(Function.identity())
                .collect(toList());
    }

    private Stream<OrphanMetaData> orphanMetaData(EntityType type) {
        Query query = new Query();
        query.fields()
                .include("data.entityid")
                .include("type")
                .include("data.metaDataFields.name:en")
                .include("data.allowedEntities.name")
                .include("data.allowedResourceServers.name")
                .include("data.stepupEntities.name")
                .include("data.disableConsent.name");

        query.addCriteria(new Criteria().orOperator(
                Criteria.where("data.allowedEntities").exists(true),
                Criteria.where("data.disableConsent").exists(true),
                Criteria.where("data.stepupEntities").exists(true),
                Criteria.where("data.allowedResourceServers").exists(true)));

        MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
        List<MetaData> metaDataWithReferences = mongoTemplate.find(query, MetaData.class, type.getType());

        Map<String, Map<String, List<MetaData>>> groupedByEntityIdReference = new HashMap<>();
        Stream.of("allowedEntities", "disableConsent", "stepupEntities", "allowedResourceServers").forEach(propertyName -> {
            metaDataWithReferences.stream().forEach(metaData -> {
                List<Map<String, Object>> entries = (List<Map<String, Object>>) metaData.getData().get(propertyName);
                if (!CollectionUtils.isEmpty(entries)) {
                    entries.forEach(ae -> groupedByEntityIdReference
                            .computeIfAbsent((String) ae.get("name"), k -> new HashMap<>())
                            .computeIfAbsent(propertyName, m -> new ArrayList<>())
                            .add(metaData));
                }
            });
        });
        List<String> types = EntityIdReconcilerHook.metaDataTypesForeignKeyRelations(type.getType());
        return groupedByEntityIdReference.entrySet().stream()
                .filter(entry -> types.stream()
                        .noneMatch(entityType -> mongoTemplate.exists(new BasicQuery("{\"data.entityid\":\"" + entry.getKey() + "\"}"), entityType)))
                .map(entry -> entry.getValue().entrySet().stream().map(m ->
                        m.getValue().stream().map(metaData -> new OrphanMetaData(
                                entry.getKey(),
                                (String) metaData.getData().get("entityid"),
                                (String) Map.class.cast(metaData.getData().get("metaDataFields")).get("name:en"),
                                m.getKey(),
                                metaData.getId(),
                                type.getType()
                        ))))
                .flatMap(Function.identity())
                .flatMap(Function.identity());
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
