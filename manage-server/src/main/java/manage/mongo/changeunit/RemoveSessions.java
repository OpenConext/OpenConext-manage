package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "removeSessions", order = "004", author = "okke.harsta@surf.nl", runAlways = true)
public class RemoveSessions {

    @Execution
    public void removeSessions(MongoTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), "sessions");
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
