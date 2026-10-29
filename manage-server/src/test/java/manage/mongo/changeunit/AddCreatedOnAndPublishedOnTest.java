package manage.mongo.changeunit;

import manage.model.EntityType;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AddCreatedOnAndPublishedOnTest extends AbstractChangeUnitTest {

    private static final Date T0 = new Date(1_000_000L);
    private static final Date T1 = new Date(2_000_000L);
    private static final Date T2 = new Date(3_000_000L);

    private final AddCreatedOnAndPublishedOn changeUnit = new AddCreatedOnAndPublishedOn();

    @Test
    void derivesDatesFromRevisionChain() {
        String sp = EntityType.SP.getType();
        // Created as testaccepted, published (prodaccepted) in the live version
        insertRaw(sp, document("chain", 2, T2, "prodaccepted"));
        insertRaw(sp.concat(REVISION_POSTFIX), revision("rev0", "chain", 0, T0, "testaccepted"));
        insertRaw(sp.concat(REVISION_POSTFIX), revision("rev1", "chain", 1, T1, "testaccepted"));
        // Created straight as prodaccepted: published on creation
        insertRaw(sp, document("direct", 0, T1, "prodaccepted"));
        // Never published
        insertRaw(sp, document("unpublished", 0, T2, "testaccepted"));

        changeUnit.addCreatedOnAndPublishedOn(mongoTemplate());

        Document chain = live(sp, "chain");
        assertEquals(T0, chain.getDate("created_on"));
        assertEquals(T2, chain.getDate("published_on"));

        Document direct = live(sp, "direct");
        assertEquals(T1, direct.getDate("created_on"));
        assertEquals(T1, direct.getDate("published_on"));

        Document unpublished = live(sp, "unpublished");
        assertEquals(T2, unpublished.getDate("created_on"));
        assertNull(unpublished.get("published_on"));
    }

    @Test
    void isIdempotent() {
        String sp = EntityType.SP.getType();
        insertRaw(sp, document("chain", 1, T1, "prodaccepted"));
        insertRaw(sp.concat(REVISION_POSTFIX), revision("rev0", "chain", 0, T0, "testaccepted"));

        changeUnit.addCreatedOnAndPublishedOn(mongoTemplate());
        changeUnit.addCreatedOnAndPublishedOn(mongoTemplate());

        Document chain = live(sp, "chain");
        assertEquals(T0, chain.getDate("created_on"));
        assertEquals(T1, chain.getDate("published_on"));
    }

    private void insertRaw(String collection, Document document) {
        mongoTemplate().getCollection(collection).insertOne(document);
    }

    private Document live(String collection, String id) {
        return mongoTemplate().getCollection(collection).find(new Document("_id", id)).first();
    }

    private Document document(String id, int number, Date created, String state) {
        return new Document("_id", id)
            .append("type", EntityType.SP.getType())
            .append("revision", new Document("number", number).append("created", created))
            .append("data", new Document("entityid", "https://" + id + ".example.org").append("eid", (long) Math.abs(id.hashCode())).append("state", state));
    }

    private Document revision(String id, String parentId, int number, Date created, String state) {
        Document document = document(id, number, created, state);
        ((Document) document.get("revision")).append("parentId", parentId);
        return document;
    }
}
