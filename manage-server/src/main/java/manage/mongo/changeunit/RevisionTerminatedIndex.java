package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.stream.Stream;
import manage.model.EntityType;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "revisionTerminatedIndex", order = "005", author = "okke.harsta@surf.nl", transactional = false)
public class RevisionTerminatedIndex {

    @Execution
    public void revisionTerminatedIndex(MongoTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            mongoTemplate.indexOps(entityType.getType().concat(REVISION_POSTFIX))
                .createIndex(new Index("revision.terminated", Sort.Direction.DESC));
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
