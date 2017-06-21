package mr.repository;

import mr.model.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.Meta;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MetaDataRepository {//extends MongoRepository<MetaData, String> {

    @Autowired
    private MongoTemplate mongoTemplate;

    public MetaData findById(String id, String type) {
        return mongoTemplate.findById(id, MetaData.class, type);
    }

    public MetaData save(MetaData metaData) {
        //TODO save history
        mongoTemplate.insert(metaData, metaData.getType());
        return metaData;
    }

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }
}
