package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.stream.Stream;
import manage.model.EntityType;
import org.springframework.data.mongodb.core.MongoTemplate;

import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "createChangeRequestsCollections", order = "010", author = "okke.harsta@surf.nl", transactional = false)
public class CreateChangeRequestsCollections {

    @Execution
    public void createChangeRequestsCollections(MongoTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            String revisionCollection = entityType.getType().concat(CHANGE_REQUEST_POSTFIX);
            if (!mongoTemplate.collectionExists(revisionCollection)) {
                mongoTemplate.createCollection(revisionCollection);
            }
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
