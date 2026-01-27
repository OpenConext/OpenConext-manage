package manage.web;

import manage.api.APIUser;
import manage.api.Scope;
import manage.exception.EndpointNotAllowed;
import manage.model.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;


public class ScopeEnforcerTest {

    @Test
    public void enforceWriteScopeIdP() {
        assertThrows(EndpointNotAllowed.class, () ->
                ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_SP)), EntityType.IDP));
    }

    @Test
    public void enforceWriteScopeSP() {
        assertThrows(EndpointNotAllowed.class, () ->
                ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_IDP)), EntityType.SP));
    }

    @Test
    public void enforceWriteScopePDP() {
        assertThrows(EndpointNotAllowed.class, () ->
                ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_SP)), EntityType.PDP));
    }

    @Test
    public void enforceWriteScopeAllowedIdP() {
        ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_IDP)), EntityType.IDP);
    }

    @Test
    public void enforceWriteScopeAllowedSP() {
        ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_SP)), EntityType.SP);
    }

    @Test
    public void enforceChangeRequestScopeIdP() {
        assertThrows(EndpointNotAllowed.class, () ->
                ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_SP)), EntityType.IDP));
    }

    @Test
    public void enforceChangeRequestScopeSP() {
        assertThrows(EndpointNotAllowed.class, () ->
                ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_IDP)), EntityType.SP));
    }

    @Test
    public void enforceChangeRequestScopePDP() {
        assertThrows(EndpointNotAllowed.class, () ->
                ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_SP)), EntityType.PDP));
    }

    @Test
    public void enforceChangeRequestScopeAllowedIdP() {
        ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_IDP)), EntityType.IDP);
    }

    @Test
    public void enforceChangeRequestScopeAllowedSP() {
        ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_SP)), EntityType.SP);
    }

    @Test
    public void enforceDeleteScopeIdP() {
        assertThrows(EndpointNotAllowed.class, () ->
                ScopeEnforcer.enforceDeleteScope(new APIUser("test", List.of(Scope.DELETE_SP)), EntityType.IDP));
    }

    @Test
    public void enforceDeleteScopeIdPNotAllowed() {
        ScopeEnforcer.enforceDeleteScope(new APIUser("test", List.of(Scope.DELETE_SP)), EntityType.SRAM);
    }

    @Test
    public void enforceDeletePolicyAllowed() {
        ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.POLICIES)), EntityType.PDP);
    }

    @Test
    public void enforceDeletePolicyNotAllowed() {
        assertThrows(EndpointNotAllowed.class, () ->
            ScopeEnforcer.enforceDeleteScope(new APIUser("test", List.of(Scope.DELETE_SP)), EntityType.PDP));
    }
}
