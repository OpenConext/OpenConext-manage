package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import manage.model.EntityType;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "addPolicy", order = "013", author = "okke.harsta@surf.nl", transactional = false)
public class AddPolicy {

    @Execution
    public void addPolicy(MongoTemplate mongoTemplate) {
        String schema = EntityType.PDP.getType();
        if (!mongoTemplate.collectionExists(schema)) {
            mongoTemplate.createCollection(schema);
        }
        String revision = schema.concat(REVISION_POSTFIX);
        if (!mongoTemplate.collectionExists(revision)) {
            mongoTemplate.createCollection(revision);
        }
        mongoTemplate.indexOps(revision).createIndex(new Index("revision.parentId", Sort.Direction.ASC));
        TextIndexDefinition textIndexDefinition = new TextIndexDefinition.TextIndexDefinitionBuilder()
            .onField("$**")
            .build();
        mongoTemplate.indexOps(schema).createIndex(textIndexDefinition);
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
