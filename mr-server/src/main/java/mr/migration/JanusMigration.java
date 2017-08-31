package mr.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.WriteResult;
import mr.conf.MetaDataAutoConfiguration;
import mr.model.MetaData;
import mr.model.Revision;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static mr.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@Component
@SuppressWarnings("unchecked")
public class JanusMigration {

    private static final Logger LOG = LoggerFactory.getLogger(JanusMigration.class);
    private String keyColumn;

    private JdbcTemplate jdbcTemplate;
    private MongoTemplate mongoTemplate;
    private ArpDeserializer arpDeserializer = new ArpDeserializer();
    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    @Autowired
    public JanusMigration(@Value("${key_column:key}") String keyColumn,
                          DataSource dataSource,
                          MongoTemplate mongoTemplate, MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.mongoTemplate = mongoTemplate;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.keyColumn = keyColumn;
    }

    public List<Map<String, Long>> doMigrate() {
        long start = System.currentTimeMillis();

        Map<String, Long> spStats = new LinkedHashMap<>();
        spStats.put("Number of SPs'", countForEntityType("janus__connection", EntityType.SP));
        spStats.put("Number of SP revisions", countForEntityType("janus__connectionRevision", EntityType.SP));

        spStats.put("Start migration of Service Providers", 0L);

        Map<String, Long> idpStats = new LinkedHashMap<>();
        idpStats.put("Number of IDPs'", countForEntityType("janus__connection", EntityType.IDP));
        idpStats.put("Number of IPD revisions", countForEntityType("janus__connectionRevision", EntityType.IDP));

        idpStats.put("Start migration of Identity Providers", 0L);

        emptyExistingCollections();

        saveEntities(EntityType.SP, spStats);
        LOG.info("Finished migration of SPs in {} ms and results {}", System.currentTimeMillis() - start, prettyPrint(spStats));
        spStats.put("Finished migration of Service Providers in ms", System.currentTimeMillis() - start);

        start = System.currentTimeMillis();
        saveEntities(EntityType.IDP, idpStats);
        LOG.info("Finished migration of IDPs in {} ms and results {}", System.currentTimeMillis() - start, prettyPrint(idpStats));
        idpStats.put("Finished migration of Identity Providers in ms", System.currentTimeMillis() - start);

        return Arrays.asList(spStats, idpStats);
    }

    private Long countForEntityType(String table, EntityType entityType) {
        return this.jdbcTemplate.queryForObject(
            "select count(*) from " + table + " where type = ?", new Object[]{entityType.getJanusDbValue()}, Long.class);
    }

    private String prettyPrint(Object obj) {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void emptyExistingCollections() {
        Set<String> schemaNames = this.metaDataAutoConfiguration.schemaNames();
        schemaNames.forEach(schema -> {
            WriteResult writeResult = mongoTemplate.remove(new Query(), schema);
            LOG.info("Deleted {} records from {}", writeResult.getN(), schema);

            String revisionSchema = schema.concat(REVISION_POSTFIX);
            writeResult = mongoTemplate.remove(new Query(), revisionSchema);
            LOG.info("Deleted {} records from {}", writeResult.getN(), revisionSchema);
        });
    }

    private void saveEntities(EntityType entityType, Map<String, Long> stats) {
        jdbcTemplate.query("SELECT CONNECTION.id, CONNECTION.revisionNr " +
                "FROM janus__connection AS CONNECTION " +
                "INNER JOIN janus__connectionRevision AS CONNECTION_REVISION ON CONNECTION_REVISION.eid = CONNECTION.id " +
                "AND CONNECTION_REVISION.revisionid = CONNECTION.revisionNr WHERE " +
                "CONNECTION.type=?",
            new String[]{entityType.getJanusDbValue()},
            rs -> {
                saveEntity(rs.getLong("id"), rs.getLong("revisionNr"), entityType.getType(), true, null, stats, entityType);
            });

    }

    private void saveEntity(Long eid, Long revisionid, String type, boolean isPrimary, String parentId, Map<String, Long> stats, EntityType entityType) {
        String sql = "SELECT janus__connectionRevision.id, janus__connectionRevision.eid, janus__connectionRevision.entityid, " +
            "janus__connectionRevision.revisionid, janus__connectionRevision.state, janus__connectionRevision.metadataurl, janus__connectionRevision.allowedall," +
            "                janus__connectionRevision.manipulation, janus__user.userid, janus__connectionRevision.created, " +
            "                janus__connectionRevision.ip, janus__connectionRevision.revisionnote, " +
            "                janus__connectionRevision.active, janus__connectionRevision.arp_attributes, " +
            "                janus__connectionRevision.notes FROM janus__connectionRevision AS janus__connectionRevision " +
            "                LEFT OUTER JOIN janus__user AS janus__user ON janus__user.uid = janus__connectionRevision.user " +
            "                WHERE  janus__connectionRevision.eid = ? AND janus__connectionRevision.revisionid = ?";
        jdbcTemplate.query(sql, new Long[]{eid, revisionid},
            rs -> {
                Map<String, Object> entity = new LinkedHashMap<>();
                entity.put("id", rs.getLong("id"));
                entity.put("eid", rs.getLong("eid"));
                entity.put("entityid", rs.getString("entityid"));
                entity.put("revisionid", rs.getLong("revisionid"));
                entity.put("state", rs.getString("state"));
                entity.put("type", entityType.getJanusDbValue());
                entity.put("metadataurl", rs.getString("metadataurl"));
                entity.put("allowedall", rs.getString("allowedall").equals("yes") ? true : false);
                entity.put("manipulation", rs.getString("manipulation"));
                String userid = rs.getString("userid");
                String user = StringUtils.hasText(userid) ? userid : "unknown";
                entity.put("user", user);
                entity.put("created", rs.getString("created"));
                entity.put("ip", rs.getString("ip"));
                entity.put("revisionnote", rs.getString("revisionnote"));
                entity.put("active", rs.getString("active").equals("yes") ? true : false);
                if (type.equals(EntityType.SP.getType())) {
                    entity.put("arp", arpDeserializer.parseArpAttributes(rs.getString("arp_attributes")));
                }
                entity.put("notes", rs.getString("notes"));
                addMetaData(entity, eid, revisionid, isPrimary, entityType);
                addAllowedEntities(entity, eid, revisionid);
                if (entityType.equals(EntityType.IDP)) {
                    addConsentDisabled(entity, eid, revisionid);
                }
                Instant instant = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse((String) entity.get("created")));
                String id = UUID.randomUUID().toString();
                MetaData metaData = new MetaData(id, 0L, type, new Revision(revisionid.intValue(), instant, parentId, user), entity);
                mongoTemplate.insert(metaData, type);
                String key = isPrimary ? entityType.getJanusDbValue() : entityType.getJanusDbValue() + "_revision";
                Long revisionCount = stats.get(key);
                stats.put(key, revisionCount == null ? 1L : revisionCount + 1);
                //now save all revisions
                if (isPrimary) {
                    jdbcTemplate.query("SELECT eid, revisionid FROM janus__connectionRevision WHERE  eid = ? AND revisionid <> ?",
                        new Long[]{eid, revisionid},
                        rs2 -> {
                            long eid1 = rs2.getLong("eid");
                            long revisionid1 = rs2.getLong("revisionid");
                            saveEntity(eid1, revisionid1, type.concat(REVISION_POSTFIX), false, id, stats, entityType);
                        });
                }
            });
    }

    private void addMetaData(Map<String, Object> entity, Long eid, Long revisionid, boolean isPrimary, EntityType entityType) {
        jdbcTemplate.query("SELECT METADATA.`" + keyColumn + "`, METADATA.`value` FROM janus__connectionRevision AS CONNECTION_REVISION " +
                "INNER JOIN janus__metadata AS METADATA ON METADATA.connectionRevisionId = CONNECTION_REVISION.id " +
                "WHERE CONNECTION_REVISION.eid = ? AND CONNECTION_REVISION.revisionid = ?",
            new Long[]{eid, revisionid},
            (rs) -> {
                parseMetaData(entity, rs.getString(keyColumn), rs.getString("value"), isPrimary, entityType);
            }
        );
    }

    private void addAllowedEntities(Map<String, Object> entity, Long eid, Long revisionid) {
        List<String> allowedEntities = jdbcTemplate.queryForList("SELECT ALLOWED_CONNECTION.name AS entityid " +
                "FROM janus__connectionRevision AS CONNECTION_REVISION " +
                "INNER JOIN `janus__allowedConnection` a ON a.connectionRevisionId = CONNECTION_REVISION.id INNER JOIN " +
                "janus__connection AS ALLOWED_CONNECTION ON ALLOWED_CONNECTION.id = a.remoteeid WHERE " +
                "CONNECTION_REVISION.eid = ? AND CONNECTION_REVISION.revisionid = ?",
            new Long[]{eid, revisionid},
            String.class
        );
        entity.put("allowedEntities", stringToNameMap(allowedEntities));
    }

    private void addConsentDisabled(Map<String, Object> entity, Long eid, Long revisionid) {
        List<String> disableConsent = jdbcTemplate.queryForList("SELECT ALLOWED_CONNECTION.name AS entityid " +
                "FROM janus__connectionRevision AS CONNECTION_REVISION " +
                "INNER JOIN janus__disableConsent disableConsent ON disableConsent.connectionRevisionId = CONNECTION_REVISION.id " +
                "INNER JOIN janus__connection AS ALLOWED_CONNECTION ON ALLOWED_CONNECTION.id = disableConsent.remoteeid " +
                "WHERE CONNECTION_REVISION.eid = ? AND CONNECTION_REVISION.revisionid = ?",
            new Long[]{eid, revisionid},
            String.class
        );
        entity.put("disableConsent", stringToNameMap(disableConsent));
    }

    private boolean keyIsPatternProperty(String key, Map<String, Object> patternProperties) {
        return patternProperties.keySet().stream().anyMatch(pattern -> Pattern.compile(pattern).matcher(key).matches());
    }

    private void parseMetaData(Map<String, Object> entity, String key, String value, boolean isPrimary, EntityType entityType) {
        if (StringUtils.hasText(value)) {
            /**
             * Prevent validation exceptions for example URI with trailing spaces
             */
            value = value.trim();
            Map<String, String> metaDataFields = (Map<String, String>) entity.getOrDefault("metaDataFields", new HashMap<String, String>());
            if (isPrimary) {
                Map<String, Object> schema = metaDataAutoConfiguration.schemaRepresentation(entityType);
                /**
                 * We only import known metadata for primary - e.g. not revisions - metadata
                 */
                Map<String, Object> metaDataSchema =
                    Map.class.cast(Map.class.cast(schema.get("properties")).get("metaDataFields"));
                Map<String, Object> properties = Map.class.cast(metaDataSchema.get("properties"));
                Map<String, Object> patternProperties = Map.class.cast(metaDataSchema.get("patternProperties"));

                if (properties.containsKey(key) || keyIsPatternProperty(key, patternProperties)) {
                    if (Pattern.compile("^contacts:([0-3]{1}):emailAddress$").matcher(key).matches()) {
                        value = value.replaceAll(Pattern.quote("mailto:"), "");
                    }
                    metaDataFields.put(key, value);
                } else {
                    LOG.info("Not adding unknown property {} with value {} for entity {} with type {}",
                        key, value, entity.get("entityid"), entityType.getType());
                }
            } else {
                metaDataFields.put(key, value);
            }
            entity.put("metaDataFields", metaDataFields);
        }
    }

    private List<Map<String, String>> stringToNameMap(List<String> entityIds) {
        return entityIds.stream().map(s -> Collections.singletonMap("name", s)).collect(toList());
    }

}
