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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@SuppressWarnings("unchecked")
public class PdPController {

    private final PolicyRepository policyRepository;
    private final String policyUrl;
    private final RestTemplate pdpRestTemplate;
    private final ObjectMapper objectMapper;
    private final MetaDataService metaDataService;

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
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/policies")
    public List<Map<String, String>> policies() {
        return policyRepository.policies();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/migrated_policies")
    public List<Map<String, String>> migratedPolicies() {
        return policyRepository.migratedPolicies();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/import_policies")
    public List<MetaData> importPolicies() throws JsonProcessingException {
        List<PdpPolicyDefinition> policyDefinitions = pdpRestTemplate.getForObject(this.policyUrl, List.class);
        String json = objectMapper.writeValueAsString(policyDefinitions);
        List<Map<String, Object>> dataList = objectMapper.readValue(json, new TypeReference<>() {
        });
        this.metaDataService.deleteCollection(EntityType.PDP);
        return dataList.stream()
                .map(data -> {
                    PdpPolicyDefinition.updateProviderStructure(data);
                    MetaData metaData = new MetaData(EntityType.PDP.getType(), data);
                    return this.metaDataService.doPost(metaData, new APIUser("PDP import", List.of(Scope.SYSTEM)), false);
                }).collect(Collectors.toList());
    }


}
