package manage.control;

import manage.api.APIUser;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import manage.service.MetaDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@SuppressWarnings("unchecked")
public class StepUpImportController {

    private static final Logger LOG = LoggerFactory.getLogger(StepUpImportController.class);

    private final MetaDataService metaDataService;
    private final MetaDataRepository metaDataRepository;

    public StepUpImportController(MetaDataService metaDataService, MetaDataRepository metaDataRepository) {
        this.metaDataService = metaDataService;
        this.metaDataRepository = metaDataRepository;
    }

    @PreAuthorize("hasAnyRole('SYSTEM')")
    @PostMapping("/internal/stepup/import/sfo")
    @Transactional
    public ResponseEntity<List<MetaData>> importStepUpServiceProviders(@RequestBody Map<String, Object> middlewareConfigJSON,
                                                                       APIUser apiUser) {
        LOG.info("importStepUpServiceProviders called by {}", apiUser.getName());

        metaDataRepository.getMongoTemplate().remove(new Query(), EntityType.SFO.getType());

        Map<String, List<Map<String, Object>>> middleware = (Map<String, List<Map<String, Object>>>) middlewareConfigJSON.get("gateway");
        List<Map<String, Object>> serviceProviders = middleware.get("service_providers");

        List<MetaData> metaDataList = serviceProviders.stream()
            .map(serviceProvider ->
                metaDataService.doPost(new MetaData(EntityType.SFO.getType(), convertServiceProviderToMetaData(serviceProvider))
                    , apiUser, false))
            .toList();
        return ResponseEntity.ok(metaDataList);
    }

    @PreAuthorize("hasAnyRole('SYSTEM')")
    @PostMapping("/internal/stepup/import/institution")
    @Transactional
    public ResponseEntity<List<MetaData>> importStepUpInstitution(@RequestBody Map<String, Map<String, Object>> middlewareInstitutionJSON,
                                                                  APIUser apiUser) {
        LOG.info("importStepUpInstitution called by {}", apiUser.getName());

        metaDataRepository.getMongoTemplate().remove(new Query(), EntityType.STEPUP.getType());

        List<MetaData> metaDataList = middlewareInstitutionJSON.entrySet().stream()
            .map(entry ->
                metaDataService.doPost(new MetaData(EntityType.STEPUP.getType(), convertInstitutionToMetaData(entry.getKey(), entry.getValue()))
                    , apiUser, false))
            .toList();
        return ResponseEntity.ok(metaDataList);
    }

    private Map<String, Object> convertInstitutionToMetaData(String entityId, Map<String, Object> institution) {
        Map<String, Object> data = new HashMap<>();
        //We need a name, which is not provided by the middleware import
        data.put("name", entityId);
        data.put("entityid", entityId);
        data.put("identifier", entityId);
        data.put("revisionnote", String.format("Imported on %s by System API", new Date()));

        data.put("use_ra_locations", institution.getOrDefault("use_ra_locations", false));
        data.put("show_raa_contact_information", institution.getOrDefault("show_raa_contact_information", false));
        data.put("verify_email", institution.getOrDefault("verify_email", false));
        data.put("number_of_tokens_per_identity", institution.getOrDefault("number_of_tokens_per_identity", 0));
        data.put("allowed_second_factors", institution.getOrDefault("allowed_second_factors", List.of()));
        data.put("use_ra", institution.getOrDefault("use_ra", List.of()));
        data.put("use_raa", institution.getOrDefault("use_raa", List.of()));
        data.put("select_raa", institution.getOrDefault("select_raa", List.of()));
        data.put("self_vet", institution.getOrDefault("self_vet", false));
        data.put("allow_self_asserted_tokens", institution.getOrDefault("allow_self_asserted_tokens", false));
        data.put("sso_on_2fa", institution.getOrDefault("sso_on_2fa", false));
        data.put("stepup-client", institution.getOrDefault("stepup-client", "full"));

        return data;
    }

    private Map<String, Object> convertServiceProviderToMetaData(Map<String, Object> serviceProvider) {
        Map<String, Object> sfo = new HashMap<>();
        //We need a name, which is not provided by the middleware import
        sfo.put("name", serviceProvider.get("entity_id"));
        sfo.put("revisionnote", String.format("Imported on %s by System API", new Date()));

        sfo.put("entityid", serviceProvider.get("entity_id"));
        sfo.put("public_key", serviceProvider.get("public_key"));
        sfo.put("acs", serviceProvider.getOrDefault("acs", List.of()));

        Map<String, String> loa = (Map<String, String>) serviceProvider.getOrDefault("loa", Map.of("__default__", "http://test.surfconext.nl/assurance/loa2"));
        sfo.put("loa", loa.get("__default__"));

        sfo.put("assertion_encryption_enabled", serviceProvider.getOrDefault("assertion_encryption_enabled", false));
        sfo.put("second_factor_only", serviceProvider.getOrDefault("second_factor_only", false));
        sfo.put("second_factor_only_nameid_patterns", serviceProvider.getOrDefault("second_factor_only_nameid_patterns", List.of()));
        sfo.put("blacklisted_encryption_algorithms", serviceProvider.getOrDefault("blacklisted_encryption_algorithms", List.of()));
        sfo.put("allow_sso_on_2fa", serviceProvider.getOrDefault("allow_sso_on_2fa", false));
        sfo.put("set_sso_cookie_on_2fa", serviceProvider.getOrDefault("set_sso_cookie_on_2fa", false));

        return sfo;
    }
}
