package manage.web;

import manage.api.APIUser;
import manage.api.Scope;
import manage.exception.EndpointNotAllowed;
import manage.model.EntityType;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ScopeEnforcerTest {

    @Test(expected = EndpointNotAllowed.class)
    public void enforceWriteScopeIdP() {
        ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_SP)), EntityType.IDP);
    }

    @Test(expected = EndpointNotAllowed.class)
    public void enforceWriteScopeSP() {
        ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_IDP)), EntityType.SP);
    }

    @Test(expected = EndpointNotAllowed.class)
    public void enforceWriteScopePDP() {
        ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_SP)), EntityType.PDP);
    }

    @Test
    public void enforceWriteScopeAllowedIdP() {
        ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_IDP)), EntityType.IDP);
    }

    @Test
    public void enforceWriteScopeAllowedSP() {
        ScopeEnforcer.enforceWriteScope(new APIUser("test", List.of(Scope.WRITE_SP)), EntityType.SP);
    }

    @Test(expected = EndpointNotAllowed.class)
    public void enforceChangeRequestScopeIdP() {
        ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_SP)), EntityType.IDP);
    }

    @Test(expected = EndpointNotAllowed.class)
    public void enforceChangeRequestScopeSP() {
        ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_IDP)), EntityType.SP);
    }

    @Test(expected = EndpointNotAllowed.class)
    public void enforceChangeRequestScopePDP() {
        ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_SP)), EntityType.PDP);
    }

    @Test
    public void enforceChangeRequestScopeAllowedIdP() {
        ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_IDP)), EntityType.IDP);
    }

    @Test
    public void enforceChangeRequestScopeAllowedSP() {
        ScopeEnforcer.enforceChangeRequestScope(new APIUser("test", List.of(Scope.CHANGE_REQUEST_SP)), EntityType.SP);
    }

    @Test(expected = EndpointNotAllowed.class)
    public void enforceDeleteScopeIdP() {
        ScopeEnforcer.enforceDeleteScope(new APIUser("test", List.of(Scope.DELETE_SP)), EntityType.IDP);
    }

    @Test
    public void enforceDeleteScopeIdPNotAllowed() {
        ScopeEnforcer.enforceDeleteScope(new APIUser("test", List.of(Scope.DELETE_SP)), EntityType.SRAM);
    }

}