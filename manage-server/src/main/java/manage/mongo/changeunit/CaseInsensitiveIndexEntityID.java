package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import java.util.stream.Stream;
import manage.model.EntityType;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.query.Collation;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "caseInsensitiveIndexEntityID", order = "006", author = "okke.harsta@surf.nl", transactional = false)
public class CaseInsensitiveIndexEntityID {

    @Execution
    public void caseInsensitiveIndexEntityID(MongoTemplate mongoTemplate) {
        Stream.of(EntityType.values())
            .filter(entityType -> !entityType.equals(EntityType.STT))
            .map(EntityType::getType).forEach(val -> {
                IndexOperations indexOperations = mongoTemplate.indexOps(val);
                List<IndexInfo> indexInfo = indexOperations.getIndexInfo();
                if (indexInfo.stream().anyMatch(info -> info.getName().equals("data.entityid_1"))) {
                    indexOperations.dropIndex("data.entityid_1");
                }
                indexOperations.createIndex(new Index("data.entityid", Sort.Direction.ASC).unique()
                    .collation(Collation.of("en").strength(2)));
            });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
