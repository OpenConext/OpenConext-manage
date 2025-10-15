package manage.control;

import lombok.SneakyThrows;
import manage.format.EngineBlockFormatter;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.PushOptions;
import manage.model.Scope;
import manage.policies.PdpPolicyDefinition;
import manage.repository.MetaDataRepository;
import manage.web.HttpHostProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
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
                       Environment environment) {
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

        this.environment = environment;
    }

    public ResponseEntity<Map> doPush(PushOptions pushOptions) {
        if (environment.acceptsProfiles(Profiles.of("dev"))) {
            return new ResponseEntity<>(Map.of(
                "eb", Map.of("status", "OK"),
                "pdp", Map.of("status", "OK"),
                "oidc", Map.of("status", "OK")
            ), HttpStatus.OK);
        }
        Map<String, Object> result = new HashMap<>();
        if (pushOptions.isIncludePdP() && pdpEnabled) {
            List<PdpPolicyDefinition> policies = pushPreviewPdP();
            this.pdpRestTemplate.put(pdpPushUri, policies);
            result.put("pdp", Map.of("status", "OK"));
        } else {
            result.put("pdp", Map.of("status", "OK"));
        }
        if (pushOptions.isIncludeEB()) {
            Map<String, Map<String, Map<String, Object>>> json = this.pushPreview();

            ResponseEntity<String> response = this.restTemplate.postForEntity(pushUri, json, String.class);

            String body = response.getBody();
            result.put("eb", Map.of(
                "status", response.getStatusCode().is2xxSuccessful() ? "OK" : "ERROR",
                "response", StringUtils.hasText(body) ? body : ""));
        } else {
            result.put("eb", Map.of("status", "OK"));
        }

        // Now push all oidc_rp metadata to OIDC proxy
        if (!environment.acceptsProfiles(Profiles.of("dev")) && oidcEnabled && pushOptions.isIncludeOIDC()) {
            List<MetaData> filteredEntities = pushPreviewOIDC();
            ResponseEntity<Void> response = this.oidcRestTemplate.postForEntity(oidcPushUri, filteredEntities, Void.class);
            result.put("oidc", Map.of(
                "status", response.getStatusCode().is2xxSuccessful() ? "OK" : "ERROR"));
        } else {
            result.put("oidc", Map.of("status", "OK"));
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
            sramServices.forEach(sramEntity -> sramEntity.metaDataFields().put("coin:collab_enabled", true));
            Map<String, Map<String, Object>> sramServicesToProvidersToPush = sramServices.stream()
                .collect(toMap(MetaData::getId, formatter::parseServiceProvider));
            serviceProvidersToPush.putAll(sramServicesToProvidersToPush);
        }
        serviceProvidersToPush.putAll(identityProvidersToPush);

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
