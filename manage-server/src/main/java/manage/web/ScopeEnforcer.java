package manage.web;

import manage.api.APIUser;
import manage.api.Scope;
import manage.exception.EndpointNotAllowed;
import manage.model.EntityType;

import java.util.List;

import static manage.api.Scope.*;

public class ScopeEnforcer {

    private final static List<EntityType> spEntityTypes = List.of(
            EntityType.SP, EntityType.RP, EntityType.RS, EntityType.SRAM);
    //Read-only EntityTypes
    private final static List<EntityType> allEntityTypes = List.of(
            EntityType.SP, EntityType.RP, EntityType.RS, EntityType.SRAM, EntityType.IDP);

    private ScopeEnforcer() {
    }

    public static void enforceWriteScope(APIUser apiUser, EntityType entityType) {
        enforceScope(entityType, apiUser, WRITE_IDP, WRITE_SP, "CRUD");
    }

    public static void enforceChangeRequestScope(APIUser apiUser, EntityType entityType) {
        enforceScope(entityType, apiUser, CHANGE_REQUEST_IDP, CHANGE_REQUEST_SP, "change request");
    }

    public static void enforceDeleteScope(APIUser apiUser, EntityType entityType) {
        if (!spEntityTypes.contains(entityType) || !apiUser.isAllowed(DELETE_SP)) {
            throw new EndpointNotAllowed(String.format("APIUser %s is not allowed to delete an entity %s", apiUser.getName(), entityType.getType()));
        }
    }

    private static void enforceScope(EntityType entityType, APIUser apiUser, Scope writeIdp, Scope writeSp, String action) {
        if (entityType.equals(EntityType.IDP) && !apiUser.isAllowed(writeIdp)) {
            throw new EndpointNotAllowed(String.format("APIUser %s is not allowed to %s for entity %s", apiUser.getName(), action, entityType.getType()));
        } else if (spEntityTypes.contains(entityType) && !apiUser.isAllowed(writeSp)) {
            throw new EndpointNotAllowed(String.format("APIUser %s is not allowed to %s for entity %s", apiUser.getName(), action, entityType.getType()));
        } else if (!allEntityTypes.contains(entityType)) {
            throw new EndpointNotAllowed(String.format("APIUser %s is not allowed to %s for entity %s", apiUser.getName(), action, entityType.getType()));
        }
    }


}
