package manage.mongo.changeunit;

import manage.AbstractIntegrationTest;
import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;
import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

/**
 * Base class for testing a single change unit: the test seeds its own, very specific data, invokes the change unit
 * directly (bypassing Mongock) and asserts on the resulting documents.
 */
public abstract class AbstractChangeUnitTest extends AbstractIntegrationTest {

    @Override
    protected boolean insertSeedData() {
        return false;
    }

    @BeforeEach
    public void clearCollections() {
        Query all = new Query();
        for (EntityType entityType : EntityType.values()) {
            String type = entityType.getType();
            mongoTemplate().remove(all, type);
            mongoTemplate().remove(all, type.concat(REVISION_POSTFIX));
            mongoTemplate().remove(all, type.concat(CHANGE_REQUEST_POSTFIX));
        }
    }

    protected MetaData insertEntity(EntityType entityType, String id, String entityId, Map<String, Object> extraData) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityid", entityId);
        data.put("state", "prodaccepted");
        data.putAll(extraData);
        MetaData metaData = new MetaData(entityType.getType(), data);
        metaData.initial(id, "test", (long) Math.abs(id.hashCode()));
        return mongoTemplate().insert(metaData, entityType.getType());
    }

    protected MetaData find(EntityType entityType, String id) {
        return mongoTemplate().findById(id, MetaData.class, entityType.getType());
    }

    protected List<MetaData> revisionsOf(EntityType entityType, String parentId) {
        return mongoTemplate().find(Query.query(Criteria.where("revision.parentId").is(parentId)), MetaData.class,
            entityType.getType().concat(REVISION_POSTFIX));
    }

    protected static Map<String, Object> entry(String name) {
        return new HashMap<>(Map.of("name", name));
    }

    protected static List<Map<String, Object>> entries(String... names) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (String name : names) {
            result.add(entry(name));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected static List<String> names(MetaData metaData, String key) {
        return ((List<Map<String, Object>>) metaData.getData().get(key)).stream()
            .map(entry -> (String) entry.get("name")).toList();
    }
}
