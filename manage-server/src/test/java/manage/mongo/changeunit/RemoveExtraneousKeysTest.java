package manage.mongo.changeunit;

import manage.model.EntityType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class RemoveExtraneousKeysTest extends AbstractChangeUnitTest {

    private final RemoveExtraneousKeys changeUnit = new RemoveExtraneousKeys();

    @Test
    void removesExtraneousKeysFromRelyingPartiesAndResourceServers() {
        insertEntity(EntityType.RP, "rp", "https://rp.example.org", Map.of("metaDataFields", Map.of(
            "scopes", List.of("openid"),
            "isResourceServer", false,
            "name:en", "RP")));
        insertEntity(EntityType.RP, "rp-clean", "https://rp-clean.example.org", Map.of("metaDataFields", Map.of(
            "name:en", "Clean RP")));
        insertEntity(EntityType.RS, "rs", "https://rs.example.org", Map.of("metaDataFields", Map.of(
            "NameIDFormats:0", "a",
            "NameIDFormats:1", "b",
            "NameIDFormats:2", "c",
            "scopes", List.of("openid"),
            "secret", "s3cr3t")));
        Long cleanVersion = find(EntityType.RP, "rp-clean").getVersion();

        changeUnit.removeExtraneousKeys(mongoTemplate());

        var rp = find(EntityType.RP, "rp").metaDataFields();
        assertFalse(rp.containsKey("scopes"));
        assertFalse(rp.containsKey("isResourceServer"));
        assertEquals("RP", rp.get("name:en"));

        // Resource servers keep their scopes, only the NameIDFormats keys go
        var rs = find(EntityType.RS, "rs").metaDataFields();
        assertFalse(rs.containsKey("NameIDFormats:0"));
        assertFalse(rs.containsKey("NameIDFormats:1"));
        assertFalse(rs.containsKey("NameIDFormats:2"));
        assertEquals(List.of("openid"), rs.get("scopes"));
        assertEquals("s3cr3t", rs.get("secret"));

        // Nothing to remove, so not saved
        assertEquals(cleanVersion, find(EntityType.RP, "rp-clean").getVersion());
    }
}
