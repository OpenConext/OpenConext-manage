package manage.mongo;

import manage.model.EntityType;
import manage.model.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"unchecked", "deprecation"})
public class MongoChangelog {

    public static final String REVISION_POSTFIX = "_revision";
    public static final String CHANGE_REQUEST_POSTFIX = "_change_request";

    private static final Logger LOG = LoggerFactory.getLogger(MongoChangelog.class);

    public static void doAddTestIndexes(MongoTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            TextIndexDefinition textIndexDefinition = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("$**")
                .build();
            mongoTemplate.indexOps(entityType.getType()).createIndex(textIndexDefinition);
        });
    }

    public static void doMigrateAllowedEntitiesToIdpSide(MongoTemplate mongoTemplate, String spCollection) {
        String idpCollection = EntityType.IDP.getType();

        // Find all SPs with allowedall = false
        List<MetaData> restrictedSps = mongoTemplate.find(
            Query.query(Criteria.where("data.allowedall").is(false)),
            MetaData.class,
            spCollection
        );
        LOG.info("Found {} {} with allowedall=false to migrate", restrictedSps.size(), spCollection);

        for (MetaData sp : restrictedSps) {
            String spEntityId = (String) sp.getData().get("entityid");

            // Collect the set of IdP entity IDs that this SP explicitly allows
            List<Map<String, Object>> spAllowedEntities = (List<Map<String, Object>>) sp.getData().getOrDefault("allowedEntities", Collections.emptyList());
            Set<String> spAllowedIdpEntityIds = spAllowedEntities.stream()
                .map(e -> (String) e.get("name"))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // Find all IdPs that have this SP in their allowedEntities list
            List<MetaData> idpsWithSp = mongoTemplate.find(
                Query.query(Criteria.where("data.allowedEntities.name").is(spEntityId)),
                MetaData.class,
                idpCollection
            );

            for (MetaData idp : idpsWithSp) {
                String idpEntityId = (String) idp.getData().get("entityid");

                // If this IdP is NOT in the SP's allowed list, the SP would have blocked it anyway,
                // so remove the SP from the IdP's allowedEntities
                if (!spAllowedIdpEntityIds.contains(idpEntityId)) {
                    List<Map<String, Object>> idpAllowedEntities = (List<Map<String, Object>>) idp.getData().get("allowedEntities");
                    idpAllowedEntities.removeIf(e -> spEntityId.equals(e.get("name")));

                    // Save previous IdP state as a revision
                    MetaData idpPrevious = mongoTemplate.findById(idp.getId(), MetaData.class, idpCollection);
                    idpPrevious.revision(UUID.randomUUID().toString());
                    mongoTemplate.insert(idpPrevious, idpCollection + REVISION_POSTFIX);

                    // Promote live IdP record
                    String idpRevisionNote = String.format(
                        "Removed %s %s from allowedEntities: it no longer manages its own allowed IdP list", spCollection, spEntityId);
                    idp.promoteToLatest("System", idpRevisionNote);
                    mongoTemplate.save(idp, idpCollection);
                    LOG.info("Removed {} {} from IdP {} allowedEntities", spCollection, spEntityId, idpEntityId);
                }
            }

            // Save previous SP state as a revision
            MetaData spPrevious = mongoTemplate.findById(sp.getId(), MetaData.class, spCollection);
            spPrevious.revision(UUID.randomUUID().toString());
            mongoTemplate.insert(spPrevious, spCollection + REVISION_POSTFIX);

            // Set SP to allowedall=true and clear allowedEntities
            sp.getData().put("allowedall", true);
            sp.getData().put("allowedEntities", new ArrayList<>());
            String spRevisionNote = "Migrated to IdP-side connection control: allowedall set to true, allowedEntities cleared";
            sp.promoteToLatest("System", spRevisionNote);
            mongoTemplate.save(sp, spCollection);
            LOG.info("{} {} migrated to allowedall=true with empty allowedEntities", spCollection, spEntityId);
        }

        LOG.info("Migration migrateAllowedEntitiesToIdpSide completed: {} {} processed", restrictedSps.size(), spCollection);
    }

    public static void migrateRelayingPartyToResourceServer(Map<String, Map<String, Object>> properties, List<Pattern> patterns, Map<String, Object> simpleProperties, MetaData rs) {
        rs.setType(EntityType.RS.getType());
        rs.getData().entrySet().removeIf(entry -> !properties.containsKey(entry.getKey()));
        rs.metaDataFields().entrySet().removeIf(entry -> !simpleProperties.containsKey(entry.getKey()) && patterns.stream().noneMatch(pattern -> pattern.matcher(entry.getKey()).matches()));
    }

    public static void doCreateSchemas(MongoTemplate mongoTemplate, List<String> connectionTypes) {
        connectionTypes.forEach(schema -> {
            if (!mongoTemplate.collectionExists(schema)) {
                mongoTemplate.createCollection(schema);
                String revision = schema.concat(REVISION_POSTFIX);
                mongoTemplate.createCollection(revision);
                mongoTemplate.indexOps(revision).createIndex(new Index("revision.parentId", Sort.Direction.ASC));
            }
        });
        connectionTypes.forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.getIndexInfo().stream()
                .filter(indexInfo -> indexInfo.getName().contains("data.eid"))
                .forEach(indexInfo -> indexOps.dropIndex(indexInfo.getName()));
            indexOps.createIndex(new Index("data.eid", Sort.Direction.ASC).unique());

            indexOps.getIndexInfo().stream().filter(indexInfo -> indexInfo.getName().contains("entityid")).forEach(indexInfo -> indexOps.dropIndex(indexInfo.getName()));
            indexOps.createIndex(new Index("data.entityid", Sort.Direction.ASC).unique()
                .collation(Collation.of("en").strength(2)));
            indexOps.createIndex(new Index("data.state", Sort.Direction.ASC));
            if (!collection.equals(EntityType.RS.getType())) {
                indexOps.createIndex(new Index("data.allowedall", Sort.Direction.ASC));
                indexOps.createIndex(new Index("data.allowedEntities.name", Sort.Direction.ASC));
                indexOps.createIndex(new Index("data.metaDataFields.coin:institution_id", Sort.Direction.ASC));
            }
        });
        connectionTypes.stream().map(s -> s + "_revision").forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.createIndex(new Index("revision.parentId", Sort.Direction.ASC));
        });
    }

}
