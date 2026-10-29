package manage.mongo.changeunit;

import manage.model.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MigrateApplicationTagsTest extends AbstractChangeUnitTest {

    private static final String TYPE_OF_SERVICE = "coin:ss:type_of_service:en";

    private final MigrateApplicationTags changeUnit = new MigrateApplicationTags();

    @Test
    void duplicatesTypeOfServiceToApplicationTags() {
        // Comma separated, case and whitespace insensitive, unmapped values are ignored
        insertEntity(EntityType.SP, "multiple", "https://multiple.example.org", fields(
            Map.of(TYPE_OF_SERVICE, " COLLABORATION , Digital Content, Unmapped")));
        insertEntity(EntityType.RP, "rp", "https://rp.example.org", fields(
            Map.of(TYPE_OF_SERVICE, "Business Processing")));
        insertEntity(EntityType.SP, "unmapped", "https://unmapped.example.org", fields(
            Map.of(TYPE_OF_SERVICE, "Unmapped")));
        insertEntity(EntityType.SP, "none", "https://none.example.org", fields(Map.of()));
        Long noneVersion = find(EntityType.SP, "none").getVersion();
        Long unmappedVersion = find(EntityType.SP, "unmapped").getVersion();

        changeUnit.migrateApplicationTags(mongoTemplate());

        assertEquals(List.of("collaboration", "content_library"),
            find(EntityType.SP, "multiple").metaDataFields().get("application_tags"));
        assertEquals(List.of("business_management"),
            find(EntityType.RP, "rp").metaDataFields().get("application_tags"));
        assertFalse(find(EntityType.SP, "unmapped").metaDataFields().containsKey("application_tags"));
        assertFalse(find(EntityType.SP, "none").metaDataFields().containsKey("application_tags"));
        // Entities without a result are not saved
        assertEquals(unmappedVersion, find(EntityType.SP, "unmapped").getVersion());
        assertEquals(noneVersion, find(EntityType.SP, "none").getVersion());
        // The original value is kept
        assertEquals(" COLLABORATION , Digital Content, Unmapped",
            find(EntityType.SP, "multiple").metaDataFields().get(TYPE_OF_SERVICE));
    }

    private Map<String, Object> fields(Map<String, Object> metaDataFields) {
        return Map.of("metaDataFields", metaDataFields);
    }
}
