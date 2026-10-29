package manage.mongo.changeunit;

import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
class ClearInvalidARPKeysTest extends AbstractChangeUnitTest {

    private static final String UID = "urn:mace:dir:attribute-def:uid";

    private final ClearInvalidARPKeys changeUnit = new ClearInvalidARPKeys();

    @Test
    void normalisesAndStripsAttributeKeys() {
        insertEntity(EntityType.SP, "dirty", "https://dirty.example.org", Map.of("arp", Map.of("enabled", true,
            "attributes", Map.of(UID, List.of(Map.of(
                "source", "idp",
                "value", "*",
                "motivation", "needed",
                "releaseAs", "uid",
                "useAsNameId", true,
                "unknown", "x"))))));

        changeUnit.clearInvalidARPKeys(mongoTemplate());

        Map<String, Object> attribute = firstAttribute(find(EntityType.SP, "dirty"));
        assertEquals(Map.of(
            "source", "idp",
            "value", "*",
            "motivation", "needed",
            "release_as", "uid",
            "use_as_nameid", true), attribute);
    }

    @Test
    void replacesListAttributesWithEmptyMap() {
        insertEntity(EntityType.RP, "list", "https://list.example.org", Map.of("arp", Map.of("enabled", true,
            "attributes", List.of())));

        changeUnit.clearInvalidARPKeys(mongoTemplate());

        Map<String, Object> arp = (Map<String, Object>) find(EntityType.RP, "list").getData().get("arp");
        assertTrue(((Map<String, Object>) arp.get("attributes")).isEmpty());
    }

    @Test
    void leavesCleanUnknownAndMissingArpUntouched() {
        insertEntity(EntityType.SP, "clean", "https://clean.example.org", Map.of("arp", Map.of("enabled", true,
            "attributes", Map.of(UID, List.of(Map.of(
                "source", "idp", "value", "*", "motivation", "m", "use_as_nameid", false))))));
        insertEntity(EntityType.SP, "unknown", "https://unknown.example.org", Map.of("arp", Map.of("enabled", true,
            "attributes", "not-a-map-or-list")));
        insertEntity(EntityType.SP, "noarp", "https://noarp.example.org", Map.of());
        Map<String, Long> versions = Map.of(
            "clean", find(EntityType.SP, "clean").getVersion(),
            "unknown", find(EntityType.SP, "unknown").getVersion(),
            "noarp", find(EntityType.SP, "noarp").getVersion());

        changeUnit.clearInvalidARPKeys(mongoTemplate());

        // The version only changes when the document is saved
        versions.forEach((id, version) -> assertEquals(version, find(EntityType.SP, id).getVersion(), id));
        assertNull(find(EntityType.SP, "noarp").getData().get("arp"));
        assertFalse(firstAttribute(find(EntityType.SP, "clean")).containsKey("release_as"));
    }

    @Test
    void isIdempotent() {
        insertEntity(EntityType.SP, "dirty", "https://dirty.example.org", Map.of("arp", Map.of("enabled", true,
            "attributes", Map.of(UID, List.of(Map.of("source", "idp", "value", "*", "releaseAs", "uid"))))));

        changeUnit.clearInvalidARPKeys(mongoTemplate());
        Long version = find(EntityType.SP, "dirty").getVersion();
        changeUnit.clearInvalidARPKeys(mongoTemplate());

        assertEquals(version, find(EntityType.SP, "dirty").getVersion());
    }

    private Map<String, Object> firstAttribute(MetaData metaData) {
        Map<String, Object> arp = (Map<String, Object>) metaData.getData().get("arp");
        Map<String, List<Map<String, Object>>> attributes = (Map<String, List<Map<String, Object>>>) arp.get("attributes");
        return attributes.get(UID).get(0);
    }
}
