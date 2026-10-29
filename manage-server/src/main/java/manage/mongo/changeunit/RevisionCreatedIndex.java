package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.stream.Stream;
import manage.model.EntityType;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "revisionCreatedIndex", order = "005", author = "okke.harsta@surf.nl", transactional = false)
public class RevisionCreatedIndex {

    @Execution
    public void revisionCreatedIndex(MongoTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            mongoTemplate.indexOps(entityType.getType())
                .createIndex(new Index("revision.created", Sort.Direction.DESC));
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
