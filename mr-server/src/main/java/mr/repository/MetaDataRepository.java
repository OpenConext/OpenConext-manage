package mr.repository;

import mr.model.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * We can't use the Spring JPA repositories as we at runtime need to decide which collection to use. We only have one
 * Document type - e.g. MetaData - and more then one MetaData collections.
 */
@Repository
public class MetaDataRepository {

    @Autowired
    private MongoTemplate mongoTemplate;

    public MetaData findById(String id, String type) {
        return mongoTemplate.findById(id, MetaData.class, type);
    }

    public MetaData save(MetaData metaData) {
        mongoTemplate.insert(metaData, metaData.getType());
        return metaData;
    }

    public List<MetaData> revisions(String type, String parentId) {
        Query query = new Query(Criteria.where("revision.parentId").is(parentId));
        return mongoTemplate.find(query, MetaData.class, type );
    }

    public void update(MetaData metaDate) {
        mongoTemplate.save(metaDate, metaDate.getType());
    }

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    public List<Map> autoComplete(String type, String search) {
        //When we have multiple types then we need to delegate depending on the type.
        Query query = new Query();
        query
            .addCriteria(new TextCriteria().matching(search))
            .fields()
            .include("data.entityid")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.name:nl");
        return mongoTemplate.find(query, Map.class, type);
    }

    public List<Map> search(String type, Map<String, Object> properties) {
        Query query = new Query();
        properties.forEach((key, value) -> query.addCriteria(Criteria.where("data.".concat(key)).is(value)));
        query
            .fields()
            .include("data.entityid")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.name:nl");
        return mongoTemplate.find(query, Map.class, type);
    }

    public List<Map> whiteListing(String type) {
        Query query = new Query();
        query
            .fields()
            .include("data.entityid")
            .include("data.allowedall")
            .include("data.allowedEntities")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.name:nl");
        return mongoTemplate.find(query, Map.class, type);
    }
}
