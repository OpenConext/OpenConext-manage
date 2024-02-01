package manage.policies;

import manage.api.APIUser;
import manage.api.ImpersonatedUser;
import manage.model.EntityType;
import manage.service.MetaDataService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static manage.service.MetaDataService.REQUESTED_ATTRIBUTES;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
@SuppressWarnings("unchecked")
public class PolicyIdpAccessEnforcer{

    private static final List<String> requiredAttributes = List.of(
            "metaDataFields.coin:institution_id",
            "entityid",
            "allowedall",
            "allowedEntities");

    private final MetaDataService metaDataService;

    public PolicyIdpAccessEnforcer(MetaDataService metaDataService) {
        this.metaDataService = metaDataService;
    }

    /**
     * Create, update or delete actions and access to the (read-only) revisions are only allowed if the
     * IdP of the signed-in user equals the IdP of the PdpPolicy or the
     * IdP of the user is linked (through the InstitutionID) to the IdP of the PdpPolicy.
     * The CUD actions are also only allowed if all the Idps of the pdpPolicy equal or are linked to the
     * AuthenticatingAuthority of the signed-in user.
     * If the Idp list of the policy is empty then the SP must have the same institutionID as the institutionID of the
     * AuthenticatingAuthority of the signed-in user.
     *
     */
    public boolean actionAllowed(PdpPolicyDefinition policy, PolicyAccess policyAccess, APIUser apiUser, boolean throwException) {
        ImpersonatedUser impersonatedUser = apiUser.getImpersonatedUser();

        if (impersonatedUser == null) {
            if (throwException) {
                throw new IllegalArgumentException("ImpersonatedUser is null for apiUser: " + apiUser.getName());
            }
            return false;
        }

        String idpEntityId = impersonatedUser.getIdpEntityId();

        List<Provider> userIdentityProviders = this.getUserIdentityProviders(idpEntityId);
        List<Provider> userServiceProviders = this.getUserServiceProviders(userIdentityProviders);

        return doInternalActionAllowed(policy, policyAccess, throwException, userIdentityProviders, userServiceProviders, impersonatedUser);
    }

    private boolean doInternalActionAllowed(PdpPolicyDefinition policy,
                                            PolicyAccess policyAccess,
                                            boolean throwException,
                                            List<Provider> userIdentityProviders,
                                            List<Provider> userServiceProviders,
                                            ImpersonatedUser impersonatedUser) {
        Set<String> userIdentityProvidersEntityIds = userIdentityProviders.stream().map(Provider::getEntityId).collect(toSet());
        Set<String> userServiceProvidersEntityIds = userServiceProviders.stream().map(Provider::getEntityId).collect(toSet());

        List<String> policyServiceProviderIds = policy.getServiceProviderIds();
        List<String> policyIdentityProviderIds = policy.getIdentityProviderIds();


        if (isEmpty(policyIdentityProviderIds)) {
            switch (policyAccess) {
                case READ:
                    //One of the policy SP must be allowed access by these users IdP
                    if (!idpIsAllowed(userIdentityProviders, policyServiceProviderIds)) {
                        if (throwException) {
                            throw new IllegalArgumentException(String.format(
                                    "Policy for target SP '%s' requested by '%s', but this SP is not allowed access by users from IdP '%s'",
                                    policyServiceProviderIds,
                                    impersonatedUser.getUnspecifiedNameId(),
                                    impersonatedUser.getIdpEntityId())
                            );
                        }
                        return false;
                    }
                    break;
                case WRITE:
                    //The SP must be owned by the IdP of the user
                    if (!userServiceProvidersEntityIds.containsAll(policyServiceProviderIds)) {
                        if (throwException) {
                            throw new IllegalArgumentException(String.format(
                                    "Policy for target SP '%s' requested by '%s', but this SP is not owned to users IdP '%s'",
                                    policyServiceProviderIds,
                                    impersonatedUser.getUnspecifiedNameId(),
                                    impersonatedUser.getIdpEntityId())
                            );
                        }
                        return false;
                    }
                    break;
            }
        } else {
            //now the SP may be anything, however all selected IDPs for this policy must be linked to this users IDP
            if (!userIdentityProvidersEntityIds.containsAll(policyIdentityProviderIds)) {
                if (throwException) {
                    throw new IllegalArgumentException(String.format(
                            "Policy for target IdPs '%s' requested by '%s', but not all are linked to users IdP '%s",
                            policyIdentityProviderIds,
                            impersonatedUser.getUnspecifiedNameId(),
                            impersonatedUser.getIdpEntityId())
                    );
                }
                return false;
            }

        }
        if (policyAccess.equals(PolicyAccess.READ)) {
            //When we get to this point the policy may be read by the user
            return true;
        }

        //finally check (e.g. for update and delete actions) if the policy is owned by this user
        String idpEntityId = impersonatedUser.getIdpEntityId();
        String authenticatingAuthorityName = policy.getAuthenticatingAuthorityName();
        if (xxx-TODO userIdentityProvidersEntityIds.contains(authenticatingAuthorityName)) {
            return true;
        }
        if (throwException) {
            throw new IllegalArgumentException(String.format(
                    "Policy created by admin '%s' of IdP '%s' can not be updated / deleted by admin '%s' of IdP '%s'",
                    policy.getUserDisplayName(),
                    authenticatingAuthorityName,
                    impersonatedUser.getUnspecifiedNameId(),
                    impersonatedUser.getIdpEntityId())
            );
        }
        return false;
    }

    /**
     * Filter out  the policies that may be seen by the user
     */
    public List<PdpPolicyDefinition> filterPdpPolicies(APIUser apiUser, List<PdpPolicyDefinition> policies) {
        ImpersonatedUser impersonatedUser = apiUser.getImpersonatedUser();

        if (impersonatedUser == null) {
            return Collections.emptyList();
        }
        String idpEntityId = impersonatedUser.getIdpEntityId();

        List<Provider> userIdentityProviders = this.getUserIdentityProviders(idpEntityId);
        List<Provider> userServiceProviders = this.getUserServiceProviders(userIdentityProviders);

        Set<String> userServiceProviderIds = userServiceProviders.stream().map(Provider::getEntityId).collect(toSet());

        List<PdpPolicyDefinition> pdpPolicyDefinitions = policies.stream()
                .filter(policy -> maySeePolicy(policy, userIdentityProviders, userServiceProviderIds))
                .collect(toList());
        //Prevent to load everything twice
        pdpPolicyDefinitions.forEach(policy ->
                policy.setActionsAllowed(this.doInternalActionAllowed(
                        policy,
                        PolicyAccess.WRITE,
                        false,
                        userIdentityProviders,
                        userServiceProviders,
                        impersonatedUser)));
        return pdpPolicyDefinitions;
    }

    /**
     * Only PdpPolicyDefinitions are returned where
     * <p>
     * the IdPs of the policy are empty and one of the SPs of the policy is allowed from through the idp's of the user
     * <p>
     * or where one of the IdPs of the policy is owned by the user
     * <p>
     * the IdPs of the policy are empty and the SP of the policy is owned by the user
     */
    private boolean maySeePolicy(PdpPolicyDefinition pdpPolicyDefinition,
                                 List<Provider> userIdentityProviders,
                                 Set<String> userServiceProviderIds) {

        Set<String> userIdentityProvidersIds = userIdentityProviders.stream().map(Provider::getEntityId).collect(toSet());

        List<String> policyIdentityProviderIds = pdpPolicyDefinition.getIdentityProviderIds();
        List<String> policyServiceProviderIds = pdpPolicyDefinition.getServiceProviderIds();

        if (isEmpty(policyIdentityProviderIds)) {
            //If there are no IdPs on the policy, then one of the SPs of the policy must be connected to the IdP of the user
            return idpIsAllowed(userIdentityProviders, policyServiceProviderIds) ||
                    policyServiceProviderIds.stream().anyMatch(spId -> userServiceProviderIds.contains(spId));
        }
        return policyIdentityProviderIds.stream().anyMatch(idp -> userIdentityProvidersIds.contains(idp));
    }

    private boolean idpIsAllowed(List<Provider> userIdentityProviders, List<String> serviceProviderIds) {
        return userIdentityProviders.stream().anyMatch(idp -> idp.isAllowedFrom(serviceProviderIds.toArray(new String[0])));
    }

    private List<Provider> getUserIdentityProviders(String entityId) {
        EntityType entityType = EntityType.IDP;
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put(REQUESTED_ATTRIBUTES, requiredAttributes);
        searchOptions.put("entityid", entityId);
        List<Provider> providers = this.metaDataService.searchEntityByType(entityType.getType(), searchOptions, false).stream()
                .map(entity -> new Provider(entityType, entity))
                .collect(toList());
        if (CollectionUtils.isEmpty(providers)) {
            return Collections.emptyList();
        }
        List<String> institutionIds = providers.stream()
                .map(provider -> provider.getInstitutionId())
                .filter(institutionId -> StringUtils.hasText(institutionId))
                .collect(toList());
        if (!CollectionUtils.isEmpty(institutionIds)) {
            searchOptions = new HashMap<>();
            searchOptions.put(REQUESTED_ATTRIBUTES, requiredAttributes);
            searchOptions.put("metaDataFields.coin:institution_id", institutionIds);
            providers = this.metaDataService.searchEntityByType(entityType.getType(), searchOptions, false).stream()
                    .map(entity -> new Provider(entityType, entity))
                    .collect(toList());
        }
        return providers;
    }

    private List<Provider> getUserServiceProviders(List<Provider> userIdentityProviders) {
        if (CollectionUtils.isEmpty(userIdentityProviders)) {
            return Collections.emptyList();
        }
        EntityType entityType = EntityType.SP;
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put(REQUESTED_ATTRIBUTES, requiredAttributes);
        searchOptions.put("metaDataFields.coin:institution_id", userIdentityProviders.get(0).getInstitutionId());
        return this.metaDataService.searchEntityByType(entityType.getType(), searchOptions, false).stream()
                .map(entity -> new Provider(entityType, entity))
                .collect(toList());
    }
}
