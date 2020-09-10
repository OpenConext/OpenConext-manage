package manage.mongo;

import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.SpringDataMongo3Driver;
import com.github.cloudyrock.spring.v5.MongockSpring5;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.MongoPersistentEntityIndexResolver;
import org.springframework.data.mongodb.core.mapping.BasicMongoPersistentEntity;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Configuration
public class MongoConvervter {

    private final MappingMongoConverter mongoConverter;
    private final MongoTemplate mongoTemplate;


    @Autowired
    public MongoConvervter(MappingMongoConverter mongoConverter, MongoTemplate mongoTemplate) {
        this.mongoConverter = mongoConverter;
        this.mongoTemplate = mongoTemplate;
    }

    @SneakyThrows
    @EventListener(ApplicationReadyEvent.class)
    @SuppressWarnings("unchecked")
    public void initIndicesAfterStartup() {
        mongoConverter.setMapKeyDotReplacement("@");
        MongoMappingContext mappingContext = (MongoMappingContext) this.mongoConverter.getMappingContext();

        for (BasicMongoPersistentEntity<?> persistentEntity : mappingContext.getPersistentEntities()) {
            Class clazz = persistentEntity.getType();
            if (clazz.isAnnotationPresent(Document.class)) {
                MongoPersistentEntityIndexResolver resolver = new MongoPersistentEntityIndexResolver(mappingContext);
                IndexOperations indexOps = mongoTemplate.indexOps(clazz);
                resolver.resolveIndexFor(clazz).forEach(indexOps::ensureIndex);
            }
        }
    }

    //
    //    // Converts . into a mongo friendly char
    //    @PostConstruct
    //    public void setUpMongoEscapeCharacterConversion() {
    //
    //    }
    //
}
