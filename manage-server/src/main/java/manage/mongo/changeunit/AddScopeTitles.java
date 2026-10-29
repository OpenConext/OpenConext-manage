package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import manage.model.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "addScopeTitles", order = "009", author = "okke.harsta@surf.nl")
public class AddScopeTitles {

    @Execution
    public void addScopeTitles(MongoTemplate mongoTemplate) {
        List<Scope> scopes = mongoTemplate.findAll(Scope.class);
        scopes.forEach(scope -> {
            scope.update(scope);
            mongoTemplate.save(scope);
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
