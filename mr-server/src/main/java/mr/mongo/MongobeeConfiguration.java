package mr.mongo;

import com.github.mongobee.Mongobee;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import mr.conf.IndexConfiguration;
import mr.conf.MetaDataAutoConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import javax.annotation.PostConstruct;
import java.util.Set;

@Configuration
@ChangeLog
public class MongobeeConfiguration {

    public static final String REVISION_POSTFIX = "_revision";

    @Autowired
    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    @Autowired
    private MappingMongoConverter mongoConverter;

    private static MetaDataAutoConfiguration staticMetaDataAutoConfiguration;


    // Converts . into a mongo friendly char
    @PostConstruct
    public void setUpMongoEscapeCharacterConversion() {
        mongoConverter.setMapKeyDotReplacement("@");
    }

    @Bean
    public Mongobee mongobee(@Value("${spring.data.mongodb.uri}") String uri) {
        Mongobee runner = new Mongobee(uri);
        runner.setChangeLogsScanPackage("mr.mongo");
        MongobeeConfiguration.staticMetaDataAutoConfiguration = metaDataAutoConfiguration;
        return runner;
    }

    @ChangeSet(order = "001", id = "createCollections", author = "Okke Harsta", runAlways = true)
    public void createCollections(MongoTemplate mongoTemplate) {
        Set<String> schemaNames = staticMetaDataAutoConfiguration.schemaNames();
        schemaNames.forEach(schema -> {
            if (!mongoTemplate.collectionExists(schema)) {
                mongoTemplate.createCollection(schema);
                staticMetaDataAutoConfiguration.indexConfigurations(schema).stream()
                    .map(this::indexDefinition)
                    .forEach(mongoTemplate.indexOps(schema)::ensureIndex);

                String revision = schema.concat(REVISION_POSTFIX);
                mongoTemplate.createCollection(revision);
                mongoTemplate.indexOps(revision).ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
            }
        });
    }

    private IndexDefinition indexDefinition(IndexConfiguration indexConfiguration) {
        switch (indexConfiguration.getType()) {
            case "text":
                return textIndex(indexConfiguration);
            case "field":
                return fieldIndex(indexConfiguration);
            default:
                throw new IllegalArgumentException("Not supported index option");
        }
    }

    private IndexDefinition fieldIndex(IndexConfiguration indexConfiguration) {
        Index index = new Index();
        indexConfiguration.getFields().forEach(field -> index.on("data.".concat(field), Sort.Direction.ASC));
        index.named(indexConfiguration.getName());
        return index;
    }

    private IndexDefinition textIndex(IndexConfiguration indexConfiguration) {
        TextIndexDefinition.TextIndexDefinitionBuilder builder = new TextIndexDefinition.TextIndexDefinitionBuilder();
        indexConfiguration.getFields().forEach(field -> builder.onField("data.".concat(field)));
        return builder.named(indexConfiguration.getName()).build();
    }
}
