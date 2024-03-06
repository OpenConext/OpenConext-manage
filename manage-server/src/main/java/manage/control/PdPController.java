package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import manage.api.APIUser;
import manage.api.Scope;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.policies.PdpPolicyDefinition;
import manage.policies.PolicyRepository;
import manage.service.MetaDataService;
import org.everit.json.schema.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@SuppressWarnings("unchecked")
public class PdPController {

    private final PolicyRepository policyRepository;
    private final String policyUrl;
    private final RestTemplate pdpRestTemplate;
    private final ObjectMapper objectMapper;
    private final MetaDataService metaDataService;
    private final HttpHeaders headers;

    public PdPController(PolicyRepository policyRepository,
                         @Value("${push.pdp.policy_url}") String policyUrl,
                         @Value("${push.pdp.user}") String pdpUser,
                         @Value("${push.pdp.password}") String pdpPassword,
                         ObjectMapper objectMapper,
                         MetaDataService metaDataService) {
        this.policyRepository = policyRepository;
        this.policyUrl = policyUrl;
        this.objectMapper = objectMapper;
        this.metaDataService = metaDataService;
        this.pdpRestTemplate = new RestTemplate();
        this.pdpRestTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(pdpUser, pdpPassword));
        this.headers = new HttpHeaders();
        this.headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/pdp/policies")
    public List<Map<String, String>> policies() {
        return policyRepository.policies();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/pdp/migrated_policies")
    public List<Map<String, String>> migratedPolicies() {
        return policyRepository.migratedPolicies();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/pdp/import_policies")
    public Map<String, List<Object>> importPolicies() throws JsonProcessingException {
        HttpEntity<?> requestEntity = new HttpEntity<>(headers);
        List<PdpPolicyDefinition> policyDefinitions = pdpRestTemplate.exchange(this.policyUrl, HttpMethod.GET, requestEntity, List.class).getBody();
        String json = objectMapper.writeValueAsString(policyDefinitions);
        List<Map<String, Object>> dataList = objectMapper.readValue(json, new TypeReference<>() {
        });
        this.metaDataService.deleteCollection(EntityType.PDP);
        Map<String, List<Object>> results = Map.of("imported", new ArrayList<>(), "errors", new ArrayList<>());
        dataList.forEach(data -> {
                    PdpPolicyDefinition.updateProviderStructure(data);
                    MetaData metaData = new MetaData(EntityType.PDP.getType(), data);
                    try {
                        MetaData savedMetaData = this.metaDataService.doPost(metaData, new APIUser("PDP import", List.of(Scope.SYSTEM)), false);
                        results.get("imported").add(savedMetaData);
                    } catch (ValidationException e) {
                        results.get("errors").add(Map.of("name", metaData.getData().get("name"), "error", e.getMessage()));
                    }
        });
        return results;
    }


}
