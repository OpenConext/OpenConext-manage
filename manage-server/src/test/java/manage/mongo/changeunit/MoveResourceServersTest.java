package manage.mongo.changeunit;

import manage.model.EntityType;
import manage.model.MetaData;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.Map;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveResourceServersTest extends AbstractChangeUnitTest {

    private final MoveResourceServers changeUnit = new MoveResourceServers();

    @Test
    void movesFlaggedRelyingPartiesAndTheirRevisionsToResourceServers() {
        insertEntity(EntityType.RP, "rs1", "https://rs1.example.org", Map.of(
            "allowedall", true,
            "metaDataFields", Map.of(
                "isResourceServer", true,
                "name:en", "Resource Server",
                "secret", "s3cr3t",
                "scopes", List.of("openid"),
                "redirectUrls", List.of("https://redirect.example.org"))));
        insertEntity(EntityType.RP, "rp1", "https://rp1.example.org", Map.of(
            "metaDataFields", Map.of("name:en", "Relying Party")));

        String rpRevisions = EntityType.RP.getType().concat(REVISION_POSTFIX);
        mongoTemplate().getCollection(rpRevisions).insertOne(revision("rs1-rev", "rs1"));
        mongoTemplate().getCollection(rpRevisions).insertOne(revision("rp1-rev", "rp1"));

        changeUnit.moveResourceServers(mongoTemplate(), metaDataAutoConfiguration);

        MetaData resourceServer = find(EntityType.RS, "rs1");
        assertNotNull(resourceServer);
        assertEquals(EntityType.RS.getType(), resourceServer.getType());
        assertEquals("https://rs1.example.org", resourceServer.getData().get("entityid"));
        // Keys that are not part of the resource server schema are dropped
        assertNull(resourceServer.getData().get("allowedall"));
        Map<String, Object> fields = resourceServer.metaDataFields();
        assertEquals("Resource Server", fields.get("name:en"));
        assertEquals("s3cr3t", fields.get("secret"));
        assertEquals(List.of("openid"), fields.get("scopes"));
        assertFalse(fields.containsKey("redirectUrls"));
        assertFalse(fields.containsKey("isResourceServer"));

        // Removed from the relying party collection, others untouched
        assertNull(find(EntityType.RP, "rs1"));
        assertNotNull(find(EntityType.RP, "rp1"));

        // Revisions follow their parent
        assertEquals(1, revisionsOf(EntityType.RS, "rs1").size());
        assertEquals(0, mongoTemplate().getCollection(rpRevisions).countDocuments(new Document("revision.parentId", "rs1")));
        assertEquals(1, mongoTemplate().getCollection(rpRevisions).countDocuments(new Document("revision.parentId", "rp1")));
    }

    @Test
    void doesNothingWithoutResourceServers() {
        insertEntity(EntityType.RP, "rp1", "https://rp1.example.org", Map.of(
            "metaDataFields", Map.of("name:en", "Relying Party")));

        changeUnit.moveResourceServers(mongoTemplate(), metaDataAutoConfiguration);

        assertNotNull(find(EntityType.RP, "rp1"));
        assertTrue(mongoTemplate().findAll(MetaData.class, EntityType.RS.getType()).isEmpty());
    }

    private Document revision(String id, String parentId) {
        return new Document("_id", id)
            .append("type", EntityType.RP.getType())
            .append("revision", new Document("number", 0).append("created", new Date()).append("parentId", parentId))
            .append("data", new Document("entityid", "https://" + id + ".example.org")
                .append("eid", (long) Math.abs(id.hashCode()))
                .append("state", "prodaccepted")
                .append("metaDataFields", new Document("name:en", "Revision")));
    }
}
