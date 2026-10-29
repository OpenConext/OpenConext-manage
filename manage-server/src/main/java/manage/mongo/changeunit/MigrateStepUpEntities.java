package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import java.util.stream.Stream;
import manage.model.EntityType;
import manage.model.MetaData;
import org.springframework.data.mongodb.core.MongoTemplate;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "migrateStepUpEntities", order = "019", author = "okke.harsta@surf.nl")
public class MigrateStepUpEntities {

    @Execution
    public void migrateStepUpEntities(MongoTemplate mongoTemplate) {
        Stream.of(EntityType.SFO.getType(), EntityType.STEPUP.getType())
            .forEach(type -> {
                List<MetaData> entities = mongoTemplate.findAll(MetaData.class, type);
                entities.forEach(metaData -> {
                    metaData.getData().remove("name");
                    mongoTemplate.save(metaData, type);
                });
            });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
