package mr.repository;

import mr.model.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * We can't use the Spring JPA repositories as we at runtime need to decide which collection to use. We only have one
 * Document type - e.g. MetaData - and more then one MetaData collections.
 */
@Repository
public class MetaDataRepository {

    private static final int AUTOCOMPLETE_LIMIT = 15;

    private MongoTemplate mongoTemplate;

    @Autowired
    public MetaDataRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public MetaData findById(String id, String type) {
        return mongoTemplate.findById(id, MetaData.class, type);
    }

    public MetaData save(MetaData metaData) {
        mongoTemplate.insert(metaData, metaData.getType());
        return metaData;
    }

    public void remove(MetaData metaData) {
        mongoTemplate.remove(metaData, metaData.getType());
    }

    public List<MetaData> revisions(String type, String parentId) {
        Query query = new Query(Criteria.where("revision.parentId").is(parentId));
        return mongoTemplate.find(query, MetaData.class, type);
    }

    public void update(MetaData metaDate) {
        mongoTemplate.save(metaDate, metaDate.getType());
    }

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    public List<Map> autoComplete(String type, String search) {
        Query query = queryWithSamlFields();
        query.limit(AUTOCOMPLETE_LIMIT);
        Criteria criteria = new Criteria();
        query.addCriteria(criteria.orOperator(
            regex("data.entityid", search),
            regex("data.metaDataFields.name:en", search),
            regex("data.metaDataFields.name:nl", search)));
        return mongoTemplate.find(query, Map.class, type);
    }

    private Criteria regex(String key, String search) {
        return Criteria.where(key).regex(".*" + search + ".*", "i");
    }

    public List<Map> search(String type, Map<String, Object> properties, List<String> requestedAttributes) {
        Query query = queryWithSamlFields();
        requestedAttributes.forEach(requestedAttribute -> {
            query.fields().include("data.".concat(requestedAttribute));
        });
        properties.forEach((key, value) -> {
            if (value instanceof String && !StringUtils.hasText(String.class.cast(value))) {
                query.addCriteria(Criteria.where("data.".concat(key)).exists(false));
            } else if ("*".equals(value)) {
                query.addCriteria(Criteria.where("data.".concat(key)).regex(".*", "i"));
            } else if (value instanceof String && String.class.cast(value).contains("*")) {
                String queryString = String.class.cast(value);
                query.addCriteria(Criteria.where("data.".concat(key)).regex(queryString, "i"));
            } else {
                query.addCriteria(Criteria.where("data.".concat(key)).is(value));
            }
        });
        return mongoTemplate.find(query, Map.class, type);
    }

    public List<Map> whiteListing(String type) {
        Query query = queryWithSamlFields();
        query.fields()
            .include("data.allowedall")
            .include("data.state")
            .include("data.allowedEntities");
        return mongoTemplate.find(query, Map.class, type);
    }

    private Query queryWithSamlFields() {
        Query query = new Query();
        //When we have multiple types then we need to delegate depending on the type.
        query
            .fields()
            .include("data.state")
            .include("data.entityid")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.name:nl");
        return query;
    }
}
