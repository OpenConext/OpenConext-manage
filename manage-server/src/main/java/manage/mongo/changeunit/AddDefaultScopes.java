package manage.mongo.changeunit;

import com.mongodb.client.DistinctIterable;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.HashMap;
import manage.model.EntityType;
import manage.model.Scope;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "addDefaultScopes", order = "003", author = "okke.harsta@surf.nl", transactional = false)
public class AddDefaultScopes {

    @Execution
    public void addDefaultScopes(MongoTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("scopes")) {
            mongoTemplate.remove(new Query(), Scope.class);
            DistinctIterable<String> scopes = mongoTemplate.getCollection(EntityType.RP.getType())
                .distinct("data.metaDataFields.scopes", String.class);
            scopes.forEach(scope -> mongoTemplate.insert(new Scope(scope, new HashMap<>(), new HashMap<>())));
        }
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
