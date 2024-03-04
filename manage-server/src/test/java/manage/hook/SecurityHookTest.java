package manage.hook;

import manage.api.APIUser;
import manage.api.Scope;
import manage.exception.EndpointNotAllowed;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.shibboleth.FederatedUser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityHookTest {

    private final SecurityHook securityHook = new SecurityHook();
    private final APIUser apiUser = new APIUser("test", emptyList());
    private final APIUser apiSystemUser = new APIUser("test", List.of(Scope.SYSTEM));
    private final FederatedUser federatedUser= new FederatedUser(List.of(Scope.SYSTEM));

    @Test
    void appliesForMetaData() {
        List.of(EntityType.RP, EntityType.SP, EntityType.IDP)
                .forEach(entityType -> assertTrue(securityHook.appliesForMetaData(new MetaData(entityType.getType(), emptyMap()))));
    }

    @Test
    void prePost() {
        MetaData metaData = new MetaData(EntityType.SP.getType(), emptyMap());
        securityHook.prePost(metaData, new APIUser("test", emptyList()));
    }

    @Test
    void prePostNotAllowed() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(), emptyMap());
        assertThrows(EndpointNotAllowed.class, () -> securityHook.prePost(metaData, apiUser));
    }

    @Test
    void prePostAllowed() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(), emptyMap());
        securityHook.prePost(metaData, federatedUser);
    }

    @Test
    void preDelete() {
    }

    @Test
    void prePutManipulation() {
        MetaData previous = new MetaData(EntityType.PROV.getType(),
                Map.of("manipulation", "v1"));
        MetaData newMetaData = new MetaData(EntityType.PROV.getType(),
                Map.of("manipulation", "v2"));
        assertThrows(EndpointNotAllowed.class, () -> securityHook.prePut(previous, newMetaData, apiUser));
        securityHook.prePut(previous, newMetaData, apiSystemUser);
    }

    @Test
    void prePutManipulationNotes() {
        MetaData previous = new MetaData(EntityType.PROV.getType(),
                Map.of("manipulationNotes", "v1"));
        MetaData newMetaData = new MetaData(EntityType.PROV.getType(),
                Map.of("manipulationNotes", "v2"));
        assertThrows(EndpointNotAllowed.class, () -> securityHook.prePut(previous, newMetaData, apiUser));
        securityHook.prePut(previous, newMetaData, apiSystemUser);
    }
}