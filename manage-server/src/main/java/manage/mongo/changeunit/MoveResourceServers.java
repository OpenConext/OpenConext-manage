package manage.mongo.changeunit;

import com.mongodb.bulk.BulkWriteResult;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.mongo.MongoChangelog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "moveResourceServers", order = "007", author = "okke.harsta@surf.nl", transactional = false)
public class MoveResourceServers {

    private static final Logger LOG = LoggerFactory.getLogger(MoveResourceServers.class);

    @SneakyThrows
    @Execution
    public void moveResourceServers(MongoTemplate mongoTemplate, MetaDataAutoConfiguration metaDataAutoConfiguration) {
        MongoChangelog.doCreateSchemas(mongoTemplate, Collections.singletonList(EntityType.RS.getType()));

        Criteria criteria = Criteria.where("data.metaDataFields.isResourceServer").is(true);
        List<MetaData> resourceServers = mongoTemplate.findAllAndRemove(Query.query(criteria), MetaData.class, EntityType.RP.getType());
        Map<String, Object> schemaRepresentation = metaDataAutoConfiguration.schemaRepresentation(EntityType.RS);
        Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) schemaRepresentation.get("properties");
        Map<String, Object> metaDataFields = properties.get("metaDataFields");
        Map<String, Object> patternProperties = (Map<String, Object>) metaDataFields.get("patternProperties");

        List<Pattern> patterns = patternProperties.keySet().stream().map(key -> Pattern.compile(key)).collect(Collectors.toList());
        Map<String, Object> simpleProperties = (Map<String, Object>) metaDataFields.get("properties");

        resourceServers.forEach(rs -> MongoChangelog.migrateRelayingPartyToResourceServer(properties, patterns, simpleProperties, rs));
        if (!CollectionUtils.isEmpty(resourceServers)) {
            BulkWriteResult bulkWriteResult = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, MetaData.class, EntityType.RS.getType()).insert(resourceServers).execute();
            LOG.info(String.format("Migrated %s relying parties to resource server collection", bulkWriteResult.getInsertedCount()));
        }

        List<String> identifiers = resourceServers.stream().map(metaData -> metaData.getId()).collect(Collectors.toList());
        Criteria revisionCriteria = Criteria.where("revision.parentId").in(identifiers);
        List<MetaData> revisions = mongoTemplate.findAllAndRemove(Query.query(revisionCriteria), MetaData.class, EntityType.RP.getType().concat(REVISION_POSTFIX));

        revisions.forEach(rev -> MongoChangelog.migrateRelayingPartyToResourceServer(properties, patterns, simpleProperties, rev));
        if (!CollectionUtils.isEmpty(revisions)) {

            BulkWriteResult bulkWriteResultRevisions = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, MetaData.class, EntityType.RS.getType().concat(REVISION_POSTFIX)).insert(revisions).execute();
            LOG.info(String.format("Migrated %s relying party revisions to resource server revisions collection", bulkWriteResultRevisions.getInsertedCount()));
        }
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
