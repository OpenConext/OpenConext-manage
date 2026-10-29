package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import manage.model.EntityType;
import manage.model.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "removeExtraneousKeys", order = "008", author = "okke.harsta@surf.nl")
public class RemoveExtraneousKeys {

    private static final Logger LOG = LoggerFactory.getLogger(RemoveExtraneousKeys.class);

    @Execution
    public void removeExtraneousKeys(MongoTemplate mongoTemplate) {
        List<MetaData> relyingParties = mongoTemplate.findAll(MetaData.class, EntityType.RP.getType());
        List<String> extraneousKeysRelyingParties = Arrays.asList("scopes", "isResourceServer");
        relyingParties.forEach(rp -> {
            Map<String, Object> metaDataFields = rp.metaDataFields();
            Set<String> keySet = metaDataFields.keySet();
            if (keySet.stream().anyMatch(key -> extraneousKeysRelyingParties.contains(key))) {
                keySet.removeIf(key -> extraneousKeysRelyingParties.contains(key));
                LOG.info(String.format("Saving %s relying party where extraneousKeys are removed", rp.getData().get("entityid")));
                mongoTemplate.save(rp, EntityType.RP.getType());
            }
        });
        List<MetaData> resourceServers = mongoTemplate.findAll(MetaData.class, EntityType.RS.getType());
        List<String> extraneousKeysResourceServers = Arrays.asList("NameIDFormats:0", "NameIDFormats:1", "NameIDFormats:2");
        resourceServers.forEach(rs -> {
            Map<String, Object> metaDataFields = rs.metaDataFields();
            Set<String> keySet = metaDataFields.keySet();
            if (keySet.stream().anyMatch(key -> extraneousKeysResourceServers.contains(key))) {
                keySet.removeIf(key -> extraneousKeysResourceServers.contains(key));
                LOG.info(String.format("Saving %s relying party where extraneousKeys are removed", rs.getData().get("entityid")));
                mongoTemplate.save(rs, EntityType.RS.getType());
            }
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
