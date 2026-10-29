package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import manage.model.EntityType;
import manage.mongo.MongoChangelog;
import org.springframework.data.mongodb.core.MongoTemplate;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "migrateAllowedEntitiesToIdpSide", order = "018", author = "okke.harsta@surf.nl")
public class MigrateAllowedEntitiesToIdpSide {

    @Execution
    public void migrateAllowedEntitiesToIdpSide(MongoTemplate mongoTemplate) {
        String spCollection = EntityType.SP.getType();
        MongoChangelog.doMigrateAllowedEntitiesToIdpSide(mongoTemplate, spCollection);
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
