package manage.policies;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import manage.api.APIUser;
import manage.api.ImpersonatedUser;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import manage.service.MetaDataService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static manage.mongo.MongoChangelog.REVISION_POSTFIX;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@SuppressWarnings("unchecked")
public class PolicyController {

    private final MetaDataService metaDataService;
    private final ObjectMapper objectMapper;
    private final PolicyIdpAccessEnforcer policyIdpAccessEnforcer;
    private final MetaDataRepository metaDataRepository;
    private final List<Map<String, String>> allowedAttributes;
    private final List<Map<String, String>> samlAllowedAttributes;
    private final List<String> loaLevels;
    private final TypeReference<List<Map<String, String>>> typeReference = new TypeReference<>() {
    };

    @SneakyThrows
    public PolicyController(MetaDataService metaDataService,
                            ObjectMapper objectMapper,
                            PolicyIdpAccessEnforcer policyIdpAccessEnforcer,
                            MetaDataRepository metaDataRepository,
                            @Value("${loa_levels}") String loaLevelsCommaSeparated) {
        this.metaDataService = metaDataService;
        this.objectMapper = objectMapper;
        this.policyIdpAccessEnforcer = policyIdpAccessEnforcer;
        this.metaDataRepository = metaDataRepository;
        this.allowedAttributes = this.attributes("policies/allowed_attributes.json");
        this.allowedAttributes.sort(Comparator.comparing(o -> o.get("label")));
        this.samlAllowedAttributes = this.attributes("policies/extra_saml_attributes.json");
        this.samlAllowedAttributes.addAll(this.allowedAttributes);
        this.samlAllowedAttributes.sort(Comparator.comparing(o -> o.get("label")));
        this.loaLevels = Stream.of(loaLevelsCommaSeparated.split(",")).map(String::trim).collect(toList());
    }

    @PreAuthorize("hasRole('POLICIES')")
    @GetMapping("/internal/protected/policies")
    public List<PdpPolicyDefinition> policies(APIUser apiUser) {
        List<PdpPolicyDefinition> policies = this.metaDataService
                .findAllByType(EntityType.PDP.getType()).stream()
                .map(metaData -> new PdpPolicyDefinition(metaData))
                .collect(toList());
        return policyIdpAccessEnforcer
                .filterPdpPolicies(apiUser, policies)
                .stream()
                .map(policy -> enrichPolicyDefinition(policy))
                .collect(toList());
    }

    @PreAuthorize("hasRole('POLICIES')")
    @GetMapping("/internal/protected/policies/{id}")
    public PdpPolicyDefinition policies(APIUser apiUser, @PathVariable("id") String id) {
        PdpPolicyDefinition policy = new PdpPolicyDefinition(this.metaDataService.getMetaDataAndValidate(EntityType.PDP.getType(), id));
        boolean actionsAllowed = policyIdpAccessEnforcer.actionAllowed(policy, PolicyAccess.READ, apiUser, true);
        policy.setActionsAllowed(actionsAllowed && !policy.getType().equals("step"));
        return enrichPolicyDefinition(policy);
    }

    @PreAuthorize("hasRole('POLICIES')")
    @PostMapping("/internal/protected/policies")
    public PdpPolicyDefinition create(APIUser apiUser, @RequestBody PdpPolicyDefinition policyDefinition) throws JsonProcessingException {
        this.initialPolicyValues(apiUser, policyDefinition, true);
        policyIdpAccessEnforcer.actionAllowed(policyDefinition, PolicyAccess.WRITE, apiUser, true);
        Map<String, Object> data = objectMapper.convertValue(policyDefinition, new TypeReference<>() {});
        PdpPolicyDefinition.updateProviderStructure(data);
        MetaData metaData = new MetaData(EntityType.PDP.getType(), data);
        MetaData metaDataSaved = this.metaDataService.doPost(metaData, apiUser, false);
        policyDefinition.setId(metaDataSaved.getId());
        return policyDefinition;
    }

    @PreAuthorize("hasRole('POLICIES')")
    @PutMapping("/internal/protected/policies")
    public PdpPolicyDefinition update(APIUser apiUser, @RequestBody PdpPolicyDefinition policyDefinition) throws JsonProcessingException {
        this.initialPolicyValues(apiUser, policyDefinition, false);
        MetaData existingMetaData = this.metaDataService.getMetaDataAndValidate(EntityType.PDP.getType(), policyDefinition.getId());
        String authenticatingAuthorityName = (String) existingMetaData.getData().get("authenticatingAuthorityName");
        //This is needed to check if the action is allowed
        policyDefinition.setAuthenticatingAuthorityName(authenticatingAuthorityName);
        policyIdpAccessEnforcer.actionAllowed(policyDefinition, PolicyAccess.WRITE, apiUser, true);

        Map<String, Object> data = objectMapper.convertValue(policyDefinition, new TypeReference<>() {});
        PdpPolicyDefinition.updateProviderStructure(data);
        existingMetaData.setData(data);
        MetaData metaData = this.metaDataService.doPut(existingMetaData, apiUser, false);
        policyDefinition.setRevisionNbr(metaData.getRevision().getNumber());
        return policyDefinition;
    }

    @PreAuthorize("hasRole('POLICIES')")
    @DeleteMapping("/internal/protected/policies/{id}")
    public void delete(APIUser apiUser, @PathVariable("id") String id) {
        PdpPolicyDefinition policyDefinition = new PdpPolicyDefinition(this.metaDataService.getMetaDataAndValidate(EntityType.PDP.getType(), id));
        this.initialPolicyValues(apiUser, policyDefinition, false);

        policyIdpAccessEnforcer.actionAllowed(policyDefinition, PolicyAccess.WRITE, apiUser, true);
        this.metaDataService.doRemove(EntityType.PDP.getType(), id, apiUser, "Deleted by dashboard API");
    }

    @PreAuthorize("hasRole('POLICIES')")
    @GetMapping("/internal/protected/revisions/{id}")
    public List<PdpPolicyDefinition> revisions(APIUser apiUser, @PathVariable("id") String id) {
        String type = EntityType.PDP.getType();
        PdpPolicyDefinition policy = new PdpPolicyDefinition(this.metaDataService.getMetaDataAndValidate(type, id));
        policyIdpAccessEnforcer.actionAllowed(policy, PolicyAccess.READ, apiUser, true);
        List<MetaData> revisions = metaDataRepository.revisions(type.concat(REVISION_POSTFIX), id);
        //Needs to be mutables
        List<PdpPolicyDefinition> pdpPolicyDefinitionRevisions = new ArrayList<>(revisions.stream().map(revision -> enrichPolicyDefinition(new PdpPolicyDefinition(revision))).collect(toList()));
        //Backward compatibility for Dashboard
        pdpPolicyDefinitionRevisions.add(policy);
        return pdpPolicyDefinitionRevisions;

    }

    @PreAuthorize("hasRole('ADMIN')")
    @RequestMapping(method = GET, value = {"/client/attributes"})
    public List<Map<String, String>> getAllowedAttributes() {
        return this.allowedAttributes;
    }

    @PreAuthorize("hasRole('POLICIES')")
    @RequestMapping(method = GET, value = { "/internal/protected/attributes"})
    public List<Map<String, String>> getAllowedAttributesForDashboard() {
        //Backward compatibility for dashboard
        return this.allowedAttributes.stream().map(attr -> Map.of("AttributeId", attr.get("value"), "Value", attr.get("label"))).collect(toList());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'POLICIES')")
    @RequestMapping(method = GET, value = {"/client/saml-attributes", "/internal/protected/saml-attributes"})
    public List<Map<String, String>> getAllowedSamlAttributes() {
        return this.samlAllowedAttributes;
    }

    @RequestMapping(method = GET, value = {"/client/loas"})
    public List<String> allowedLevelOfAssurances() {
        return this.loaLevels;
    }


    private PdpPolicyDefinition enrichPolicyDefinition(PdpPolicyDefinition policyDefinition) {
        List<String> serviceProviderIds = policyDefinition.getServiceProviderIds();
        if (!CollectionUtils.isEmpty(serviceProviderIds)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("entityid", serviceProviderIds);
            List<Map> serviceProviders = metaDataService.searchEntityByType(EntityType.SP.getType(), properties, false);
            //we might be missing some RP's
            if (serviceProviders.size() < serviceProviderIds.size()) {
                List<String> allEntityIDs = serviceProviders.stream().map(entity -> entityID(entity)).collect(toList());
                List<String> rpProviderIds = serviceProviderIds.stream().filter(id -> !allEntityIDs.contains(id)).collect(toList());
                properties.put("entityid", rpProviderIds);
                List<Map> relyingParties = metaDataService.searchEntityByType(EntityType.RP.getType(), properties, false);
                serviceProviders.addAll(relyingParties);
            }
            policyDefinition.setServiceProviderNames(serviceProviders.stream().map(sp -> name(sp)).collect(toList()));
            policyDefinition.setServiceProviderNamesNl(serviceProviders.stream().map(sp -> nameNL(sp)).collect(toList()));
            policyDefinition.setActivatedSr(serviceProviders.stream().allMatch(sp -> policyEnforcementDecisionRequired(sp)));
        }
        List<String> identityProviderIds = policyDefinition.getIdentityProviderIds();
        if (!CollectionUtils.isEmpty(identityProviderIds)) {
            Map<String, Object> properties = new HashMap<>();
            properties.put("entityid", identityProviderIds);
            List<Map> identityProviders = metaDataService.searchEntityByType(EntityType.IDP.getType(), properties, false);
            policyDefinition.setIdentityProviderNames(identityProviders.stream().map(idp -> name(idp)).collect(toList()));
            policyDefinition.setIdentityProviderNamesNl(identityProviders.stream().map(idp -> nameNL(idp)).collect(toList()));
        }
        if (policyDefinition.getType().equals("step")) {
            policyDefinition.getLoas().forEach(loa -> loa.getCidrNotations()
                    .forEach(notation -> notation.setIpInfo(IPAddressProvider.getIpInfo(notation.getIpAddress(), notation.getPrefix()))));
            policyDefinition.setActionsAllowed(false);
        }
        return policyDefinition;
    }

    private void initialPolicyValues(APIUser apiUser, PdpPolicyDefinition policyDefinition, boolean includeAuthenticatingAuthority) {
        ImpersonatedUser impersonatedUser = apiUser.getImpersonatedUser();

        if (impersonatedUser == null) {
            throw new IllegalArgumentException("ImpersonatedUser is null for apiUser: " + apiUser.getName());
        }
        if (includeAuthenticatingAuthority) {
            policyDefinition.setAuthenticatingAuthorityName(impersonatedUser.getIdpEntityId());
        }
        policyDefinition.setUserDisplayName(impersonatedUser.getUnspecifiedNameId());
    }

    private List<Map<String, String>> attributes(String path) throws IOException {
        return this.objectMapper.readValue(new ClassPathResource(path).getInputStream(), typeReference);
    }

    private String entityID(Map<String, Object> entity) {
        return (String) ((Map)entity.get("data")).get("entity");
    }

    private boolean policyEnforcementDecisionRequired(Map<String, Object> entity) {
        return (boolean) ((Map)((Map)entity.get("data")).get("metaDataFields")).getOrDefault("coin:policy_enforcement_decision_required", false);
    }

    private String name(Map<String, Object> entity) {
        return (String) ((Map)((Map)entity.get("data")).get("metaDataFields")).get("name:en");
    }

    private String nameNL(Map<String, Object> entity) {
        return (String) ((Map)((Map)entity.get("data")).get("metaDataFields")).get("name:nl");
    }
}
