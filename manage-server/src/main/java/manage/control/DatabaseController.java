package manage.control;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import manage.format.EngineBlockFormatter;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.PushOptions;
import manage.model.Scope;
import manage.policies.PdpPolicyDefinition;
import manage.repository.MetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
@RestController
@SuppressWarnings("unchecked")
public class DatabaseController {

    private static final Logger LOG = LoggerFactory.getLogger(DatabaseController.class);

    private static final int BATCH_SIZE = 500;

    private final RestTemplate restTemplate;
    private final String pushUri;

    private final RestTemplate oidcRestTemplate;
    private final String oidcPushUri;
    private final boolean oidcEnabled;

    private final RestTemplate stepUpRestTemplate;
    private final String stepUpPushUri;
    private final boolean stepUpEnabled;
    private final Map<String, Object> stepUpConfiguration;

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
                       @Value("${push.oidc.enabled}") boolean oidcEnabled,
                       @Value("${push.pdp.user}") String pdpUser,
                       @Value("${push.pdp.password}") String pdpPassword,
                       @Value("${push.pdp.url}") String pdpPushUri,
                       @Value("${push.pdp.enabled}") boolean pdpEnabled,
                       @Value("${push.stepup.enabled}") boolean stepUpEnabled,
                       @Value("${push.stepup.url}") String stepUpPushUri,
                       @Value("${push.stepup.user}") String stepUpUser,
                       @Value("${push.stepup.password}") String stepUpPassword,
                       @Value("${push.stepup.configuration_file}") Resource stepupConfigurationResource,
                       ObjectMapper objectMapper,
                       Environment environment) throws IOException {
        this.metaDataRepository = metaDataRepository;
        this.pushUri = pushUri;

        this.restTemplate = RestTemplateIdiom.buildRestTemplate(pushUri, user, password);
        this.excludeEduGainImported = excludeEduGainImported;
        this.excludeOidcRP = excludeOidcRP;
        this.excludeSRAM = excludeSRAM;

        this.oidcRestTemplate = RestTemplateIdiom.buildRestTemplate(oidcPushUri, oidcUser, oidcPassword);
        this.oidcPushUri = oidcPushUri;
        this.oidcEnabled = oidcEnabled;

        this.pdpRestTemplate = RestTemplateIdiom.buildRestTemplate(pdpPushUri, pdpUser, pdpPassword);
        this.pdpPushUri = pdpPushUri;
        this.pdpEnabled = pdpEnabled;

        this.stepUpRestTemplate = RestTemplateIdiom.buildRestTemplate(stepUpPushUri, stepUpUser, stepUpPassword);
        this.stepUpPushUri = stepUpPushUri;
        this.stepUpEnabled = stepUpEnabled;
        this.stepUpConfiguration = objectMapper.readValue(stepupConfigurationResource.getInputStream(), new TypeReference<>() {
        });

        this.environment = environment;
    }

    public ResponseEntity<Map> doPush(PushOptions pushOptions) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            return new ResponseEntity<>(Map.of(
                "eb", Map.of("status", "OK"),
                "pdp", Map.of("status", "OK"),
                "oidc", Map.of("status", "OK"),
                "stepup", Map.of("status", "OK")
            ), HttpStatus.OK);
        }
        Map<String, Object> result = new HashMap<>();
        List<PdpPolicyDefinition> policies = pushPreviewPdP();
        if (pushOptions.isIncludePdP() && pdpEnabled) {
            try {
                this.pdpRestTemplate.put(pdpPushUri, policies);
                result.put("pdp", Map.of("status", "OK"));
            } catch (HttpStatusCodeException e) {
                String message = String.format("Error in push to PDP (%s) status %s and response %s",
                    pdpPushUri, e.getStatusCode(), e.getResponseBodyAsString());
                LOG.error(message);
                return new ResponseEntity<>(Map.of("message", message), HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                String message = String.format("Error in push to PDP (%s) error %s",
                    pdpPushUri, e.getMessage());
                LOG.error(message);
                return new ResponseEntity<>(Map.of("message", message), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            result.put("pdp", Map.of("status", "OK"));
        }
        if (pushOptions.isIncludeEB()) {
            Map<String, Map<String, Map<String, Object>>> json = this.doPushPreview(policies);

            try {
                ResponseEntity<String> response = this.restTemplate.postForEntity(pushUri, json, String.class);

                String body = response.getBody();
                result.put("eb", Map.of(
                    "status", response.getStatusCode().is2xxSuccessful() ? "OK" : "ERROR",
                    "response", StringUtils.hasText(body) ? body : ""));
            } catch (HttpStatusCodeException e) {
                String message = String.format("Error in push to EngineBlock (%s) status %s and response %s",
                    pushUri, e.getStatusCode(), e.getResponseBodyAsString());
                LOG.error(message);
                return new ResponseEntity<>(Map.of("message", message), HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                String message = String.format("Error in push to EngineBlock (%s) error %s",
                    pushUri, e.getMessage());
                LOG.error(message);
                return new ResponseEntity<>(Map.of("message", message), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            result.put("eb", Map.of("status", "OK"));
        }

        // Now push all oidc_rp metadata to OIDC proxy
        if (oidcEnabled && pushOptions.isIncludeOIDC()) {
            List<MetaData> filteredEntities = pushPreviewOIDC();
            try {
                ResponseEntity<Void> response = this.oidcRestTemplate.postForEntity(oidcPushUri, filteredEntities, Void.class);
                result.put("oidc", Map.of(
                    "status", response.getStatusCode().is2xxSuccessful() ? "OK" : "ERROR"));
            } catch (HttpStatusCodeException e) {
                String message = String.format("Error in push to OIDC (%s) status %s and response %s",
                    oidcPushUri, e.getStatusCode(), e.getResponseBodyAsString());
                LOG.error(message);
                return new ResponseEntity<>(Map.of("message", message), HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                String message = String.format("Error in push to OIDC (%s) error %s",
                    oidcPushUri, e.getMessage());
                LOG.error(message);
                return new ResponseEntity<>(Map.of("message", message), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            result.put("oidc", Map.of("status", "OK"));
        }

        if (stepUpEnabled && pushOptions.isIncludeStepUp()) {
            Map<String, Map<String, Object>> institutions = pushPreviewInstitution();
            Map<String, Object> stepUpConfiguration = pushPreviewSFO();
            Map<String, List<String>> stepUpWhiteList = pushPreviewStepup();
            Map<String, Object> stepUpEndPoint = Map.of(
                "/management/institution-configuration", institutions,
                "/management/configuration", stepUpConfiguration,
                "/management/whitelist/replace", stepUpWhiteList
            );
            try {
                //The ordering matters
                List<String> ordering = List.of(
                    "/management/whitelist/replace",
                    "/management/institution-configuration",
                    "/management/configuration"
                    );
                ordering.forEach(key -> {
                    ResponseEntity<Map> response = this.stepUpRestTemplate.postForEntity(
                        stepUpPushUri + key,
                        stepUpEndPoint.get(key),
                        Map.class);
                    boolean successFul = response.getStatusCode().is2xxSuccessful();
                    result.put("stepup", Map.of("status", successFul ? "OK" : "ERROR"));

                });
            } catch (HttpStatusCodeException e) {
                String message = String.format("Error in push to Stepup (%s) status %s and response %s",
                    stepUpPushUri, e.getStatusCode(), e.getResponseBodyAsString());
                LOG.error(message);
                return new ResponseEntity<>(Map.of("message", message), HttpStatus.INTERNAL_SERVER_ERROR);
            } catch (Exception e) {
                String message = String.format("Error in push to Stepup (%s) error %s",
                    stepUpPushUri, e.getMessage());
                LOG.error(message);
                return new ResponseEntity<>(Map.of("message", message), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            result.put("stepup", Map.of("status", "OK"));
        }
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    private List<PdpPolicyDefinition> pushPreviewPdP() {
        List<PdpPolicyDefinition> policies = this.metaDataRepository
            .findAllByType(EntityType.PDP.getType()).stream()
            .map(PdpPolicyDefinition::new)
            .filter(PdpPolicyDefinition::isActive)
            .collect(toList());
        return policies;
    }

    private Map<String, Object> pushPreviewSFO() {
        List<MetaData> sfoEntities = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.SFO.getType());
        Map<String, Object> results = new HashMap<>();
        if (stepUpConfiguration.containsKey("sraa")) {
            results.put("sraa", stepUpConfiguration.get("sraa"));
        }
        if (stepUpConfiguration.containsKey("email_templates")) {
            results.put("email_templates", stepUpConfiguration.get("email_templates"));
        }
        List<Map<String, Object>> serviceProviders = sfoEntities.stream()
            .map(MetaData::getData)
            .map(data -> Map.of(
                "entity_id", data.get("entityid"),
                "public_key", data.get("public_key"),
                "acs", data.getOrDefault("acs", List.of()),
                "loa", Map.of("__default__", data.get("loa")),
                "assertion_encryption_enabled", data.getOrDefault("assertion_encryption_enabled", false),
                "second_factor_only", data.getOrDefault("second_factor_only", false),
                "second_factor_only_nameid_patterns", data.getOrDefault("second_factor_only_nameid_patterns", List.of()),
                "blacklisted_encryption_algorithms", data.getOrDefault("blacklisted_encryption_algorithms", List.of()),
                "allow_sso_on_2fa", data.getOrDefault("allow_sso_on_2fa", false),
                "set_sso_cookie_on_2fa", data.getOrDefault("set_sso_cookie_on_2fa", false)
            ))
            .toList();
        results.put("gateway", Map.of("service_providers", serviceProviders, "identity_providers", List.of()));
        return results;
    }

    private Map<String, List<String>> pushPreviewStepup() {
        List<MetaData> institutions = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.STEPUP.getType());
        return Map.of(
            "institutions",
            institutions.stream()
                .map(MetaData::getData)
                .filter(data -> "full".equals(data.get("stepup-client")))
                .map(data -> (String) data.get("identifier"))
                .toList()
        );
    }

    private Map<String, Map<String, Object>> pushPreviewInstitution() {
        List<MetaData> institutions = metaDataRepository.getMongoTemplate().findAll(MetaData.class, EntityType.STEPUP.getType());
        List<String> sfoAttributes = getSfoAttributes();

        return institutions.stream().map(MetaData::getData)
            .collect(toMap(
                data -> (String) data.get("identifier"),
                data -> data.keySet().stream()
                    .filter(key -> sfoAttributes.contains(key))
                    .collect(toMap(
                        key -> key,
                        key -> data.get(key)
                    ))
            ));
    }

    private List<String> getSfoAttributes() {
        List<String> properties = new ArrayList<>();
        properties.add("use_ra_locations");
        properties.add("show_raa_contact_information");
        properties.add("verify_email");
        properties.add("allowed_second_factors");
        properties.add("number_of_tokens_per_identity");
        properties.add("use_ra");
        properties.add("use_raa");
        properties.add("select_raa");
        properties.add("self_vet");
        properties.add("allow_self_asserted_tokens");
        properties.add("sso_on_2fa");
        properties.add("use_ra_locations");
        return properties;
    }

    private List<MetaData> pushPreviewOIDC() {
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
                .findAll(MetaData.class, EntityType.SRAM.getType());
            sramRelyingParties.forEach(sramEntity -> sramEntity.metaDataFields().put("coin:collab_enabled", true));
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
        return filteredEntities;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/pushPreview")
    public Map<String, Map<String, Map<String, Object>>> pushPreview() {
        List<PdpPolicyDefinition> policies = pushPreviewPdP();
        return doPushPreview(policies);
    }

    private Map<String, Map<String, Map<String, Object>>> doPushPreview(List<PdpPolicyDefinition> policies) {

        EngineBlockFormatter formatter = new EngineBlockFormatter();

        List<MetaData> serviceProviders = metaDataRepository.getMongoTemplate().stream(
            new Query().cursorBatchSize(BATCH_SIZE),
            MetaData.class,
            EntityType.SP.getType()
        ).toList();

        Stream<MetaData> spStream = serviceProviders.parallelStream();
        if (excludeEduGainImported) {
            spStream = spStream.filter(metaData -> {
                Map<String, Object> fields = metaData.metaDataFields();
                boolean imported = Boolean.TRUE.equals(fields.get("coin:imported_from_edugain"));
                boolean push = Boolean.TRUE.equals(fields.get("coin:push_enabled"));
                return !imported || push;
            });
        }

        Map<String, Map<String, Object>> serviceProvidersToPush =
            spStream.filter(metaData -> !excludeFromPush(metaData.metaDataFields()))
                .collect(Collectors.toConcurrentMap(
                    MetaData::getId,
                    formatter::parseServiceProvider
                ));

        List<MetaData> identityProviders = metaDataRepository.getMongoTemplate().stream(
            new Query().cursorBatchSize(BATCH_SIZE),
            MetaData.class,
            EntityType.IDP.getType()
        ).toList();

        filterOutNullDisableConsentExplanations(identityProviders);

        Map<String, Map<String, Object>> identityProvidersToPush =
            identityProviders.parallelStream()
                .filter(metaData -> !excludeFromPush(metaData.metaDataFields()))
                .collect(Collectors.toConcurrentMap(
                    MetaData::getId,
                    formatter::parseIdentityProvider
                ));

        if (!excludeOidcRP) {
            List<MetaData> relyingParties = metaDataRepository.getMongoTemplate().stream(
                new Query().cursorBatchSize(BATCH_SIZE),
                MetaData.class,
                EntityType.RP.getType()
            ).toList();

            Map<String, Map<String, Object>> oidcClientsToPush =
                relyingParties.parallelStream()
                    .filter(metaData -> !excludeFromPush(metaData.metaDataFields()))
                    .collect(Collectors.toConcurrentMap(
                        MetaData::getId,
                        formatter::parseOidcClient
                    ));

            serviceProvidersToPush.putAll(oidcClientsToPush);
        }

        if (!excludeSRAM) {
            List<MetaData> sramServices = metaDataRepository.getMongoTemplate().stream(
                new Query().cursorBatchSize(BATCH_SIZE),
                MetaData.class,
                EntityType.SRAM.getType()
            ).toList();

            sramServices.forEach(sram -> sram.metaDataFields().put("coin:collab_enabled", true));

            Map<String, Map<String, Object>> sramToPush =
                sramServices.parallelStream()
                    .collect(Collectors.toConcurrentMap(
                        MetaData::getId,
                        formatter::parseServiceProvider
                    ));

            serviceProvidersToPush.putAll(sramToPush);
        }

        // Add IdPs to the same map, as EB looks at the type: saml20-sp or saml20-idp
        serviceProvidersToPush.putAll(identityProvidersToPush);

        Set<String> allServiceProviderIds = policies.parallelStream()
            .filter(p -> p.isActive() && !p.isIdpPolicy())
            .flatMap(p -> p.getServiceProviderIds().stream())
            .collect(Collectors.toSet());

        Set<String> allIdentityProviderIds = policies.parallelStream()
            .filter(p -> p.isActive() && p.isIdpPolicy())
            .flatMap(p -> p.getIdentityProviderIds().stream())
            .collect(Collectors.toSet());

        serviceProvidersToPush.values().parallelStream().forEach(provider -> {
            String type = (String) provider.get("type");
            Map<String, Object> metadata = (Map<String, Object>) provider.computeIfAbsent("metadata", k -> new HashMap<>());
            Map<String, Object> coin = (Map<String, Object>) metadata.computeIfAbsent("coin", k -> new HashMap<>());
            String entityID = (String) provider.getOrDefault("name", "");

            boolean addDecision = type.equals(EntityType.SP.getJanusDbValue())
                ? allServiceProviderIds.contains(entityID)
                : allIdentityProviderIds.contains(entityID);

            if (addDecision) {
                coin.put("policy_enforcement_decision_required", "1");
            } else {
                coin.remove("policy_enforcement_decision_required");
            }
        });

        Map<String, Map<String, Map<String, Object>>> results = new HashMap<>();
        results.put("connections", serviceProvidersToPush);
        return results;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/pushPreviewOIDC")
    public List<MetaData> pushPreviewOIDCEndpoint() {
        return this.pushPreviewOIDC();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/pushPreviewPdP")
    public List<PdpPolicyDefinition> pushPreviewPdPEndpoint() {
        return this.pushPreviewPdP();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/pushPreviewSFO")
    public Map<String, Object> pushPreviewSFOEndpoint() {
        return this.pushPreviewSFO();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/pushPreviewInstitution")
    public Map<String, Map<String, Object>> pushPreviewInstitutionEndpoint() {
        return this.pushPreviewInstitution();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/pushPreviewStepup")
    public Map<String, List<String>> pushPreviewStepupEndpoint() {
        return this.pushPreviewStepup();
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


}
