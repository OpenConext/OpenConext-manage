package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import manage.model.EntityType;
import manage.mongo.MongoChangelog;
import org.springframework.data.mongodb.core.MongoTemplate;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "migrateAllowedEntitiesToIdpSideForRP", order = "021", author = "okke.harsta@surf.nl")
public class MigrateAllowedEntitiesToIdpSideForRP {

    @Execution
    public void migrateAllowedEntitiesToIdpSideForRP(MongoTemplate mongoTemplate) {
        String rpCollection = EntityType.RP.getType();
        MongoChangelog.doMigrateAllowedEntitiesToIdpSide(mongoTemplate, rpCollection);
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
