package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import manage.model.EntityType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "addTextIndexRS", order = "012", author = "okke.harsta@surf.nl", transactional = false)
public class AddTextIndexRS {

    @Execution
    public void addTextIndexRS(MongoTemplate mongoTemplate) {
        String schema = EntityType.RS.getType();
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
