package manage.mongo.changeunit;

import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class MigrateMultiplicityKeysTest extends AbstractChangeUnitTest {

    private final MigrateMultiplicityKeys changeUnit = new MigrateMultiplicityKeys();

    @Test
    void migratesNameAndKeywordsToMultiplicityKeys() {
        insertEntity(EntityType.IDP, "idp", "https://idp.example.org", Map.of("metaDataFields", Map.of(
            "name:en", "Name EN",
            "name:nl", "Naam NL",
            "keywords:en", "kw en",
            "keywords:pt", "kw pt",
            "coin:other", "untouched")));

        changeUnit.migrateMultiplicityKeys(mongoTemplate());

        Map<String, Object> fields = find(EntityType.IDP, "idp").metaDataFields();
        assertEquals("Name EN", fields.get("DiscoveryName:0:en"));
        assertEquals("Naam NL", fields.get("DiscoveryName:0:nl"));
        assertFalse(fields.containsKey("DiscoveryName:0:pt"));
        assertEquals("kw en", fields.get("keywords:0:en"));
        assertEquals("kw pt", fields.get("keywords:0:pt"));
        assertFalse(fields.containsKey("keywords:en"));
        assertFalse(fields.containsKey("keywords:pt"));
        // The original name is kept
        assertEquals("Name EN", fields.get("name:en"));
        assertEquals("untouched", fields.get("coin:other"));
    }

    @Test
    void isIdempotent() {
        insertEntity(EntityType.IDP, "idp", "https://idp.example.org", Map.of("metaDataFields", Map.of(
            "name:en", "Name EN", "keywords:en", "kw en")));

        changeUnit.migrateMultiplicityKeys(mongoTemplate());
        MetaData first = find(EntityType.IDP, "idp");
        changeUnit.migrateMultiplicityKeys(mongoTemplate());

        assertEquals(first.metaDataFields(), find(EntityType.IDP, "idp").metaDataFields());
    }
}
