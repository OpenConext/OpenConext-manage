package manage.mongo.changeunit;

import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeduplicateIdpEntityListsTest extends AbstractChangeUnitTest {

    private final DeduplicateIdpEntityLists changeUnit = new DeduplicateIdpEntityLists();

    @Test
    void removesDuplicatesKeepsFirstOccurrenceAndStoresRevision() {
        insertEntity(EntityType.IDP, "dirty", "https://dirty.example.org", Map.of(
            "allowedEntities", entities("a", "b", "a"),
            "disableConsent", entities("x", "x"),
            "stepupEntities", entities("s")));
        insertEntity(EntityType.IDP, "clean", "https://clean.example.org", Map.of(
            "allowedEntities", entities("a", "b")));

        changeUnit.deduplicateIdpEntityLists(mongoTemplate());

        MetaData dirty = find(EntityType.IDP, "dirty");
        assertEquals(java.util.List.of("a", "b"), names(dirty, "allowedEntities"));
        assertEquals(java.util.List.of("x"), names(dirty, "disableConsent"));
        assertEquals(java.util.List.of("s"), names(dirty, "stepupEntities"));
        assertEquals(1, dirty.getRevision().getNumber());
        assertTrue(((String) dirty.getData().get("revisionnote")).contains("allowedEntities (1 removed)"));

        var revisions = revisionsOf(EntityType.IDP, "dirty");
        assertEquals(1, revisions.size());
        assertEquals(java.util.List.of("a", "b", "a"), names(revisions.get(0), "allowedEntities"));

        MetaData clean = find(EntityType.IDP, "clean");
        assertEquals(0, clean.getRevision().getNumber());
        assertEquals(0, revisionsOf(EntityType.IDP, "clean").size());
    }

    @Test
    void isIdempotent() {
        insertEntity(EntityType.IDP, "dirty", "https://dirty.example.org", Map.of(
            "allowedEntities", entities("a", "a")));

        changeUnit.deduplicateIdpEntityLists(mongoTemplate());
        changeUnit.deduplicateIdpEntityLists(mongoTemplate());

        assertEquals(1, revisionsOf(EntityType.IDP, "dirty").size());
        assertEquals(1, find(EntityType.IDP, "dirty").getRevision().getNumber());
    }

    private static java.util.List<Map<String, Object>> entities(String... names) {
        return entries(names);
    }
}
