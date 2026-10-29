package manage.mongo.changeunit;

import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrateAllowedEntitiesToIdpSideTest extends AbstractChangeUnitTest {

    @Test
    void serviceProviders() {
        verifyMigration(EntityType.SP, () -> new MigrateAllowedEntitiesToIdpSide()
            .migrateAllowedEntitiesToIdpSide(mongoTemplate()));
    }

    @Test
    void relyingParties() {
        verifyMigration(EntityType.RP, () -> new MigrateAllowedEntitiesToIdpSideForRP()
            .migrateAllowedEntitiesToIdpSideForRP(mongoTemplate()));
    }

    private void verifyMigration(EntityType type, Runnable migration) {
        // Restricted: only allows idpA
        insertEntity(type, "restricted", "https://restricted.example.org", Map.of(
            "allowedall", false,
            "allowedEntities", entries("https://idpA.example.org")));
        // Unrestricted: must be left alone
        insertEntity(type, "open", "https://open.example.org", Map.of("allowedall", true));
        // idpA is allowed by the restricted SP, so keeps it. idpB is not, so the entry must be removed
        insertEntity(EntityType.IDP, "idpA", "https://idpA.example.org", Map.of(
            "allowedEntities", entries("https://restricted.example.org")));
        insertEntity(EntityType.IDP, "idpB", "https://idpB.example.org", Map.of(
            "allowedEntities", entries("https://restricted.example.org", "https://open.example.org")));

        migration.run();

        MetaData restricted = find(type, "restricted");
        assertEquals(true, restricted.getData().get("allowedall"));
        assertTrue(((List<?>) restricted.getData().get("allowedEntities")).isEmpty());
        assertEquals(1, restricted.getRevision().getNumber());
        assertEquals(1, revisionsOf(type, "restricted").size());

        MetaData open = find(type, "open");
        assertEquals(0, open.getRevision().getNumber());
        assertEquals(0, revisionsOf(type, "open").size());

        MetaData idpA = find(EntityType.IDP, "idpA");
        assertEquals(List.of("https://restricted.example.org"), names(idpA, "allowedEntities"));
        assertEquals(0, revisionsOf(EntityType.IDP, "idpA").size());

        MetaData idpB = find(EntityType.IDP, "idpB");
        assertEquals(List.of("https://open.example.org"), names(idpB, "allowedEntities"));
        assertEquals(1, revisionsOf(EntityType.IDP, "idpB").size());

        // Second run: nothing is restricted anymore, so nothing changes
        migration.run();
        assertEquals(1, revisionsOf(type, "restricted").size());
        assertEquals(1, revisionsOf(EntityType.IDP, "idpB").size());
    }
}
