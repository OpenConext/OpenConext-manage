package mr.repository;

import mr.model.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

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

    public void update(MetaData metaDate) {
        mongoTemplate.save(metaDate, metaDate.getType());
    }

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}
