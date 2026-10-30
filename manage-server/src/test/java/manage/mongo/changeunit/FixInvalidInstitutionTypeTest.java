package manage.mongo.changeunit;

import manage.model.EntityType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@SuppressWarnings("unchecked")
class FixInvalidInstitutionTypeTest extends AbstractChangeUnitTest {

    private final FixInvalidInstitutionType changeUnit = new FixInvalidInstitutionType();

    @Test
    void replacesInvalidValueAndKeepsRevision() {
        insertIdp("invalid", Map.of("coin:institution_type", "Foo"));

        changeUnit.fixInvalidInstitutionType(mongoTemplate());

        assertEquals("Overig hoger onderwijs", institutionType("invalid"));
        assertEquals(1, revisionsOf(EntityType.IDP, "invalid").size());
    }

    @Test
    void leavesValidEmptyAndMissingUntouched() {
        insertIdp("valid", Map.of("coin:institution_type", "MBO"));
        insertIdp("empty", Map.of("coin:institution_type", ""));
        insertIdp("missing", Map.of());

        changeUnit.fixInvalidInstitutionType(mongoTemplate());

        assertEquals("MBO", institutionType("valid"));
        assertEquals("", institutionType("empty"));
        assertFalse(((Map<String, Object>) find(EntityType.IDP, "missing").getData().get("metaDataFields"))
            .containsKey("coin:institution_type"));
        assertEquals(0, revisionsOf(EntityType.IDP, "valid").size());
    }

    private void insertIdp(String id, Map<String, Object> fields) {
        insertEntity(EntityType.IDP, id, "https://" + id + ".example.org",
            Map.of("metaDataFields", new HashMap<>(fields)));
    }

    private Object institutionType(String id) {
        return ((Map<String, Object>) find(EntityType.IDP, id).getData().get("metaDataFields"))
            .get("coin:institution_type");
    }
}
