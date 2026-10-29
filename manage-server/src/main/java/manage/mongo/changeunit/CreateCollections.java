package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.Arrays;
import manage.mongo.MongoChangelog;
import manage.mongo.Sequence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "createCollections", order = "001", author = "okke.harsta@surf.nl", transactional = false)
public class CreateCollections {

    private static final Logger LOG = LoggerFactory.getLogger(CreateCollections.class);

    @Execution
    public void createCollections(MongoTemplate mongoTemplate) {
        MongoChangelog.doCreateSchemas(mongoTemplate, Arrays.asList("saml20_sp", "saml20_idp", "oidc10_rp"));
        if (!mongoTemplate.collectionExists("sequences")) {
            LOG.info("Creating sequence collection with new start seq {}", 999L);

            mongoTemplate.createCollection("sequences");
            mongoTemplate.save(new Sequence("sequence", 999L));
        }

    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
