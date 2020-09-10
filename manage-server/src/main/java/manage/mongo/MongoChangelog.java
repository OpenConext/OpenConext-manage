package manage.mongo;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate;
import com.mongodb.client.DistinctIterable;
import manage.conf.IndexConfiguration;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Query;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Configuration
@ChangeLog
@SuppressWarnings("unchecked")
public class MongoChangelog {

    public static final String REVISION_POSTFIX = "_revision";

    private static final Logger LOG = LoggerFactory.getLogger(MongoChangelog.class);

    private static MetaDataAutoConfiguration staticMetaDataAutoConfiguration;

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    @Autowired
    public MongoChangelog(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        MongoChangelog.staticMetaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @ChangeSet(order = "001", id = "createCollections", author = "Okke Harsta")
    public void createCollections(MongockTemplate mongoTemplate) {
        this.doCreateSchemas(mongoTemplate, Arrays.asList("saml20_sp", "saml20_idp"));
        if (!mongoTemplate.collectionExists("sequences")) {
            LOG.info("Creating sequence collection with new start seq {}", 1L);

            mongoTemplate.createCollection("sequences");
            mongoTemplate.save(new Sequence("sequence", 1L));
        }

    }

    @ChangeSet(order = "002", id = "addTextIndexes", author = "Okke Harsta")
    public void addTextIndexes(MongockTemplate mongoTemplate) {
        doAddTestIndexes(mongoTemplate);
    }

    private void doAddTestIndexes(MongockTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            TextIndexDefinition textIndexDefinition = new TextIndexDefinition.TextIndexDefinitionBuilder()
                    .onField("$**")
                    .build();
            mongoTemplate.indexOps(entityType.getType()).ensureIndex(textIndexDefinition);
        });
    }

    @ChangeSet(order = "003", id = "createOIDCSchema", author = "Okke Harsta")
    public void createOIDCSchema(MongockTemplate mongoTemplate) {
        doCreateSchemas(mongoTemplate, Arrays.asList("oidc10_rp"));
    }

    @ChangeSet(order = "004", id = "addDefaultScopes", author = "Okke Harsta")
    public void addDefaultScopes(MongockTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("scopes")) {
            mongoTemplate.remove(new Query(), Scope.class);
            DistinctIterable<String> scopes = mongoTemplate.getCollection(EntityType.RP.getType())
                    .distinct("data.metaDataFields.scopes", String.class);
            List<Scope> allScopes = StreamSupport.stream(scopes.spliterator(), false)
                    .map(scope -> new Scope(scope, new HashMap<>()))
                    .collect(Collectors.toList());
            mongoTemplate.insert(allScopes, Scope.class);
        }
    }

    @ChangeSet(order = "005", id = "removeSessions", author = "Okke Harsta", runAlways = true)
    public void removeSessions(MongockTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), "sessions");
    }

    @ChangeSet(order = "006", id = "uniqueScopeName", author = "Okke Harsta")
    public void uniqueScopeName(MongockTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), "sessions");
    }

    private void doCreateSchemas(MongockTemplate mongoTemplate, List<String> connectionTypes) {
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
        connectionTypes.forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.getIndexInfo().stream()
                    .filter(indexInfo -> indexInfo.getName().contains("data.eid"))
                    .forEach(indexInfo -> indexOps.dropIndex(indexInfo.getName()));
            indexOps.ensureIndex(new Index("data.eid", Sort.Direction.ASC).unique());
            if (indexOps.getIndexInfo().stream().anyMatch(indexInfo -> indexInfo.getName().equals("field_entityid"))) {
                indexOps.dropIndex("field_entityid");
            }
            indexOps.ensureIndex(new Index("data.entityid", Sort.Direction.ASC).unique());
            indexOps.ensureIndex(new Index("data.state", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("data.allowedall", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("data.allowedEntities.name", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("metaDataFields.coin:institution_id", Sort.Direction.ASC));
        });
        connectionTypes.stream().map(s -> s + "_revision").forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
        });
    }

    private IndexDefinition indexDefinition(IndexConfiguration indexConfiguration) {
        Index index = new Index();
        indexConfiguration.getFields().forEach(field -> index.on("data.".concat(field), Sort.Direction.ASC));
        index.named(indexConfiguration.getName());
        if (indexConfiguration.isUnique()) {
            index.unique();
        }
        return index;
    }

}
