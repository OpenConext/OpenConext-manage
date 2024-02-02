package manage.policies;

import manage.api.APIUser;
import manage.api.ImpersonatedUser;
import manage.exception.EndpointNotAllowed;
import manage.service.MetaDataService;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class PolicyIdpAccessEnforcerTest {

    private final MetaDataService metaDataService = Mockito.mock(MetaDataService.class);
    private final PolicyIdpAccessEnforcer accessEnforcer = new PolicyIdpAccessEnforcer(metaDataService);

    @Test(expected = EndpointNotAllowed.class)
    public void actionAllowedMissingUser() {
        accessEnforcer.actionAllowed(new PdpPolicyDefinition(), PolicyAccess.WRITE, new APIUser(), true);
    }

    @Test
    public void actionAllowedMissingUserIndicate() {
        assertFalse(accessEnforcer.actionAllowed(new PdpPolicyDefinition(), PolicyAccess.WRITE, new APIUser(), false));
    }

    @Test
    public void actionNotAllowedForSPOnlyPolicy() {
        PdpPolicyDefinition policy = new PdpPolicyDefinition();
        policy.setServiceProviderIds(List.of("http://not-owned"));
        when(metaDataService.searchEntityByType(any(), anyMap(), anyBoolean()))
                .thenReturn(List.of(
                        this.provider("http://mock-idp", List.of(), "nope", false)
                ));
        boolean allowed = accessEnforcer.actionAllowed(policy, PolicyAccess.READ, this.apiUser("nope"), false);
        assertFalse(allowed);

        allowed = accessEnforcer.actionAllowed(policy, PolicyAccess.WRITE, this.apiUser("nope"), false);
        assertFalse(allowed);
    }

    @Test
    public void actionNotAllowedForIdPPolicy() {
        PdpPolicyDefinition policy = new PdpPolicyDefinition();
        policy.setIdentityProviderIds(List.of("http://not-owned"));
        when(metaDataService.searchEntityByType(any(), anyMap(), anyBoolean()))
                .thenReturn(List.of(
                        this.provider("http://mock-idp", List.of(), "nope", false)
                ));
        boolean allowed = accessEnforcer.actionAllowed(policy, PolicyAccess.READ, this.apiUser("nope"), false);
        assertFalse(allowed);
    }

    @Test
    public void actionNotAllowedForAuthenticatingAuthority() {
        PdpPolicyDefinition policy = new PdpPolicyDefinition();
        policy.setIdentityProviderIds(List.of("http://mock-idp"));
        policy.setAuthenticatingAuthorityName("nope");
        when(metaDataService.searchEntityByType(any(), anyMap(), anyBoolean()))
                .thenReturn(List.of(
                        this.provider("http://mock-idp", List.of(), "nope", false)
                ));
        boolean allowed = accessEnforcer.actionAllowed(policy, PolicyAccess.WRITE, this.apiUser("nope"), false);
        assertFalse(allowed);
    }

    @Test
    public void actionAllowedForAuthenticatingAuthority() {
        PdpPolicyDefinition policy = new PdpPolicyDefinition();
        policy.setIdentityProviderIds(List.of("http://mock-idp"));
        policy.setAuthenticatingAuthorityName("http://mock-idp");
        when(metaDataService.searchEntityByType(any(), anyMap(), anyBoolean()))
                .thenReturn(List.of(
                        this.provider("http://mock-idp", List.of(), "nope", false)
                ));
        boolean allowed = accessEnforcer.actionAllowed(policy, PolicyAccess.WRITE, this.apiUser("nope"), false);
        assertTrue(allowed);
    }

    @Test
    public void filterPdpPoliciesWithoutImpersonatedUser() {
        List<PdpPolicyDefinition> policies = accessEnforcer.filterPdpPolicies(
                new APIUser(),
                List.of(new PdpPolicyDefinition()));
        assertEquals(0, policies.size());
    }

    @Test
    public void filterPdpPoliciesWithServiceProvider() {
        PdpPolicyDefinition policy = new PdpPolicyDefinition();
        policy.setServiceProviderIds(List.of("http://mock-idp"));
        when(metaDataService.searchEntityByType(any(), anyMap(), anyBoolean()))
                .thenReturn(List.of(
                        this.provider("http://mock-idp", List.of(), "nope", false)
                ));
        List<PdpPolicyDefinition> policies = accessEnforcer.filterPdpPolicies(
                apiUser("http://mock-idp"),
                List.of(policy));
        assertEquals(1, policies.size());
    }

    @Test
    public void filterPdpPoliciesWithIdentityProvider() {
        PdpPolicyDefinition policy = new PdpPolicyDefinition();
        policy.setIdentityProviderIds(List.of("http://mock-idp"));
        when(metaDataService.searchEntityByType(any(), anyMap(), anyBoolean()))
                .thenReturn(List.of(
                        this.provider("http://mock-idp", List.of(), "nope", false)
                ));
        List<PdpPolicyDefinition> policies = accessEnforcer.filterPdpPolicies(
                apiUser("http://mock-idp"),
                List.of(policy));
        assertEquals(1, policies.size());
    }

    private Map<String, Object> provider(String entityId,
                                         List<String> allowedEntities,
                                         String institutionId,
                                         boolean allowedAll) {
        return Map.of("data", Map.of(
                "entityid", entityId,
                "allowedEntities", allowedEntities.stream().map(s -> Map.of("name", s)).collect(Collectors.toList()),
                "metaDataFields", Map.of(
                        "coin:institution_id", institutionId,
                        "allowedall", allowedAll
                )
        ));
    }

    private APIUser apiUser(String idpEntityId) {
        APIUser apiUser = new APIUser("JD", List.of());
        apiUser.setImpersonatedUser(new ImpersonatedUser(idpEntityId, "unr:john", "JD"));
        return apiUser;
    }
}