package manage.mongo.changeunit;

import manage.model.EntityType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("unchecked")
class MigrateInstitutionTypesTest extends AbstractChangeUnitTest {

    private final MigrateInstitutionTypes changeUnit = new MigrateInstitutionTypes();

    @Test
    void migratesAllOldValuesAndCreatesRevisions() {
        int i = 0;
        for (String oldValue : MigrateInstitutionTypes.MAPPING.keySet()) {
            insertIdp("idp" + i++, oldValue);
        }
        changeUnit.migrateInstitutionTypes(mongoTemplate());

        i = 0;
        for (Map.Entry<String, String> entry : MigrateInstitutionTypes.MAPPING.entrySet()) {
            String id = "idp" + i++;
            assertEquals(entry.getValue(), institutionType(id), entry.getKey());
            // Unchanged values (MBO) are skipped and don't get a revision
            assertEquals(entry.getKey().equals(entry.getValue()) ? 0 : 1, revisionsOf(EntityType.IDP, id).size());
        }
    }

    @Test
    void leavesNewEmptyAndMissingUntouched() {
        insertIdp("valid", "Hogeschool");
        insertIdp("empty", "");
        insertEntity(EntityType.IDP, "missing", "https://missing.example.org",
            Map.of("metaDataFields", new HashMap<>()));

        changeUnit.migrateInstitutionTypes(mongoTemplate());

        assertEquals("Hogeschool", institutionType("valid"));
        assertEquals("", institutionType("empty"));
        assertEquals(0, revisionsOf(EntityType.IDP, "valid").size());
        assertEquals(0, revisionsOf(EntityType.IDP, "empty").size());
        assertEquals(0, revisionsOf(EntityType.IDP, "missing").size());
    }

    @Test
    void unknownValueFallsBackToUniversiteit() {
        insertIdp("unknown", "Foo");
        changeUnit.migrateInstitutionTypes(mongoTemplate());
        assertEquals("Universiteit", institutionType("unknown"));
    }

    private void insertIdp(String id, String value) {
        insertEntity(EntityType.IDP, id, "https://" + id + ".example.org",
            Map.of("metaDataFields", new HashMap<>(Map.of("coin:institution_type", value))));
    }

    private Object institutionType(String id) {
        return ((Map<String, Object>) find(EntityType.IDP, id).getData().get("metaDataFields"))
            .get("coin:institution_type");
    }
}
