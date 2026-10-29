package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import manage.mongo.MongoChangelog;
import org.springframework.data.mongodb.core.MongoTemplate;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "addTextIndexes", order = "002", author = "okke.harsta@surf.nl", transactional = false)
public class AddTextIndexes {

    @Execution
    public void addTextIndexes(MongoTemplate mongoTemplate) {
        MongoChangelog.doAddTestIndexes(mongoTemplate);
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
