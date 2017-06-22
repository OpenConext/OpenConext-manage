package mr.mongo;

import com.github.mongobee.Mongobee;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClientURI;
import mr.conf.MetadataAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.util.StringUtils;

import java.util.Set;

@Configuration
@ChangeLog
public class MongobeeConfiguration {

    public static final String REVISION_POSTFIX = "_revision";

    @Autowired
    private MetadataAutoConfiguration metadataAutoConfiguration;

    private static ThreadLocal<MetadataAutoConfiguration> metadataAutoConfigurationHolder;

    @Bean
    public Mongobee mongobee(@Value("${spring.data.mongodb.uri}") String uri){
        Mongobee runner = new Mongobee(uri);
        runner.setChangeLogsScanPackage("mr.mongo");

        metadataAutoConfigurationHolder = new ThreadLocal<>();
        metadataAutoConfigurationHolder.set(this.metadataAutoConfiguration);

        return runner;
    }

    @ChangeSet(order = "001", id = "createCollections", author = "Okke Harsta")
    public void createCollections(MongoTemplate mongoTemplate){
        Set<String> schemaNames = metadataAutoConfigurationHolder.get().schemaNames();
        schemaNames.forEach(schema -> {
            if (!mongoTemplate.collectionExists(schema)) {
                mongoTemplate.createCollection(schema);
                String revision = schema.concat(REVISION_POSTFIX);
                mongoTemplate.createCollection(revision);
                mongoTemplate.indexOps(revision).ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
            }
        });
    }
}
