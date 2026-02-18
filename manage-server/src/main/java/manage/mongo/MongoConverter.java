package manage.mongo;

import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

@Configuration
public class MongoConverter {

    private final MappingMongoConverter mongoConverter;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public MongoConverter(MappingMongoConverter mongoConverter, MongoTemplate mongoTemplate) {
        this.mongoConverter = mongoConverter;
        this.mongoTemplate = mongoTemplate;
    }

    @SneakyThrows
    @EventListener(ApplicationReadyEvent.class)
    public void initIndicesAfterStartup() {
        mongoConverter.setMapKeyDotReplacement("@");
        MongoMappingContext mappingContext = (MongoMappingContext) this.mongoConverter.getMappingContext();
        MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);

        mappingContext.getPersistentEntities().forEach(persistentEntity -> {
            Class<?> clazz = persistentEntity.getType();
            if (clazz.isAnnotationPresent(Document.class)) {
                IndexOperations indexOps = mongoTemplate.indexOps(clazz);
                resolver.resolveIndexFor(clazz).forEach(indexOps::createIndex);
            }
        });
    }
}
