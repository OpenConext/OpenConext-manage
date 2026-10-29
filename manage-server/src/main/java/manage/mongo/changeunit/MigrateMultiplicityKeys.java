package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import java.util.Map;
import manage.model.EntityType;
import manage.model.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "migrateMultiplicityKeys", order = "016", author = "okke.harsta@surf.nl")
public class MigrateMultiplicityKeys {

    private static final Logger LOG = LoggerFactory.getLogger(MigrateMultiplicityKeys.class);

    @Execution
    public void migrateMultiplicityKeys(MongoTemplate mongoTemplate) {
        List<MetaData> identityProviders = mongoTemplate.findAll(MetaData.class, EntityType.IDP.getType());
        identityProviders.forEach(metaData -> {
            Map<String, Object> metaDataFields = metaData.metaDataFields();
            List.of("en", "nl", "pt").forEach(language -> {
                String name = (String) metaDataFields.get("name:" + language);
                if (StringUtils.hasText(name)) {
                    metaDataFields.put("DiscoveryName:0:" + language, name);
                }
                String keywords = (String) metaDataFields.get("keywords:" + language);
                if (StringUtils.hasText(keywords)) {
                    metaDataFields.put("keywords:0:" + language, keywords);
                }
                metaDataFields.remove("keywords:" + language);
            });
            //We don't make a new revision as this is a cleanup action
            LOG.info(String.format("Saving %s metadata type %s where DiscoveryName and keywords are migrated",
                metaData.getData().get("entityid"), EntityType.IDP.getType()));
            mongoTemplate.save(metaData, EntityType.IDP.getType());
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
