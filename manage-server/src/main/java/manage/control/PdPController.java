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
import manage.repository.MetaDataRepository;
import manage.service.MetaDataService;
import org.everit.json.schema.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static manage.model.EntityType.*;

@RestController
@SuppressWarnings("unchecked")
public class PdPController {

    private final PolicyRepository policyRepository;
    private final String policyUrl;
    private final String decideUrl;
    private final RestTemplate pdpRestTemplate;
    private final ObjectMapper objectMapper;
    private final MetaDataService metaDataService;
    private final MetaDataRepository metaDataRepository;
    private final HttpHeaders headers;
    private final Pattern descriptionPattern = Pattern.compile("<Description>(.+?)</Description>", Pattern.DOTALL);

    public PdPController(PolicyRepository policyRepository,
                         @Value("${push.pdp.policy_url}") String policyUrl,
                         @Value("${push.pdp.decide_url}") String decideUrl,
                         @Value("${push.pdp.user}") String pdpUser,
                         @Value("${push.pdp.password}") String pdpPassword,
                         ObjectMapper objectMapper,
                         MetaDataService metaDataService,
                         MetaDataRepository metaDataRepository) {
        this.policyRepository = policyRepository;
        this.policyUrl = policyUrl;
        this.decideUrl = decideUrl;
        this.objectMapper = objectMapper;
        this.metaDataService = metaDataService;
        this.metaDataRepository = metaDataRepository;
        this.pdpRestTemplate = new RestTemplate();
        this.pdpRestTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(pdpUser, pdpPassword));
        this.headers = new HttpHeaders();
        this.headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/pdp/policies")
    public List<Map<String, String>> policies() {
        List<Map<String, String>> policies = policyRepository.policies();
        addDescription(policies);
        return policies;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/pdp/migrated_policies")
    public List<Map<String, String>> migratedPolicies() {
        List<Map<String, String>> policies = policyRepository.migratedPolicies();
        addDescription(policies);
        return policies;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value =  "/client/pdp/decide")
    public String decideManage(@RequestBody String payload) {
        HttpEntity<?> requestEntity = new HttpEntity<>(payload, headers);
        return pdpRestTemplate.exchange(this.decideUrl, HttpMethod.POST, requestEntity, String.class).getBody();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value =  "/client/pdp/missing-enforcements")
    public List<MetaData> policiesWithMissingPolicyEnforcementDecisionRequired() {
        return metaDataRepository.policiesWithMissingPolicyEnforcementDecisionRequired();
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
            Map<String, List<String>> missingReferences = this.missingReferences(metaData);
            if (missingReferences.values().stream().anyMatch(references -> !references.isEmpty())) {
                results.get("errors").add(Map.of("name", metaData.getData().get("name"), "error", missingReferences.toString()));
            } else {
                try {
                    MetaData savedMetaData = this.metaDataService.doPost(metaData, new APIUser("PDP import", List.of(Scope.SYSTEM)), false);
                    results.get("imported").add(savedMetaData);
                } catch (ValidationException e) {
                    results.get("errors").add(Map.of("name", metaData.getData().get("name"), "error", e.getMessage()));
                }
            }
        });
        return results;
    }

    private void addDescription(List<Map<String, String>> policies) {
        policies.forEach(policy -> {
            Matcher matcher = descriptionPattern.matcher(policy.get("xml"));
            matcher.find();
            policy.put("description", matcher.group(1));
        });
    }

    private Map<String, List<String>> missingReferences(MetaData newMetaData) {
        String serviceProviderIds = "serviceProviderIds";
        String identityProviderIds = "identityProviderIds";
        Map<String, List<EntityType>> relationsToCheck = Map.of(
                serviceProviderIds, Arrays.asList(SP, RP),
                identityProviderIds, singletonList(IDP)
        );
        Map<String, List<String>> missingReferences = Map.of(
                serviceProviderIds, new ArrayList<>(),
                identityProviderIds, new ArrayList<>()
        );
        relationsToCheck.forEach((key, value) -> {
            if (newMetaData.getData().containsKey(key)) {
                List<Map<String, String>> references = (List<Map<String, String>>) newMetaData.getData().get(key);
                if (!CollectionUtils.isEmpty(references)) {
                    List<String> missingProviders = references.stream()
                            .filter(map -> value.stream()
                                    .allMatch(entityType ->
                                            CollectionUtils.isEmpty(
                                                    metaDataRepository.findRaw(entityType.getType(),
                                                            String.format("{\"data.entityid\" : \"%s\"}", map.get("name")))))
                            ).map(m -> m.get("name"))
                            .collect(toList());
                    missingReferences.get(key).addAll(missingProviders);
                }
            }
        });
        return missingReferences;
    }

}
