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
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
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

import static java.util.stream.Collectors.toList;
import static mr.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@Component
public class JanusMigration implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(JanusMigration.class);

    private JdbcTemplate jdbcTemplate;
    private MongoTemplate mongoTemplate;
    private ArpDeserializer arpDeserializer = new ArpDeserializer();
    private MetaDataAutoConfiguration metaDataAutoConfiguration;
    private boolean migrate;

    @Autowired
    public JanusMigration(@Value("${migrate_data_from_janus}") boolean migrate, DataSource dataSource, MongoTemplate mongoTemplate, MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.mongoTemplate = mongoTemplate;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.migrate = migrate;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (migrate) {
            doMigrate();
        }
    }

    public List<Map<String, Long>> doMigrate() {
        long start =System.currentTimeMillis();

        Map<String, Long> spStats = new HashMap<>();
        Map<String, Long> idpStats = new HashMap<>();

        emptyExistingCollections();

        saveEntities(EntityType.SP, spStats);
        LOG.info("Finished migration of SPs in {} ms and results {}", System.currentTimeMillis() - start, prettyPrint(spStats));

        start =System.currentTimeMillis();
        saveEntities(EntityType.IDP, idpStats);
        LOG.info("Finished migration of IDPs in {} ms and results {}", System.currentTimeMillis() - start, prettyPrint(spStats));
        return Arrays.asList(spStats, idpStats);
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
                saveEntity(rs.getLong("id"), rs.getLong("revisionNr"), entityType.getType(), true, null, stats);
            });

    }

    private void saveEntity(Long eid, Long revisionid, String type, boolean isPrimary, String parentId, Map<String, Long> stats) {
        jdbcTemplate.query("SELECT id, eid, entityid, revisionid, state, metadataurl, allowedall, " +
                "manipulation, user, created, ip, revisionnote, active, arp_attributes, notes FROM janus__connectionRevision " +
                "WHERE  eid = ? AND revisionid = ?",
            new Long[]{eid, revisionid},
            rs -> {
                Map<String, Object> entity = new LinkedHashMap<>();
                entity.put("id", rs.getLong("id"));
                entity.put("eid", rs.getLong("eid"));
                entity.put("entityid", rs.getString("entityid"));
                entity.put("revisionid", rs.getLong("revisionid"));
                entity.put("state", rs.getString("state"));
                entity.put("metadataurl", rs.getString("metadataurl"));
                entity.put("allowedall", rs.getString("allowedall").equals("yes") ? true : false);
                entity.put("manipulation", rs.getString("manipulation"));
                entity.put("user", rs.getString("user"));
                entity.put("created", rs.getString("created"));
                entity.put("ip", rs.getString("ip"));
                entity.put("revisionnote", rs.getString("revisionnote"));
                entity.put("active", rs.getString("active").equals("yes") ? true : false);
                if (type.equals(EntityType.SP.getType())) {
                    entity.put("arp", arpDeserializer.parseArpAttributes(rs.getString("arp_attributes")));
                }
                entity.put("notes", rs.getString("notes"));
                addMetaData(entity, eid, revisionid);
                addAllowedEntities(entity, eid, revisionid);
                addConsentDisabled(entity, eid, revisionid);
                Instant instant = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse((String) entity.get("created")));
                String id = UUID.randomUUID().toString();
                MetaData metaData = new MetaData(id, type, new Revision(revisionid.intValue(), instant, parentId, (String) entity.get("user")), entity);
                mongoTemplate.insert(metaData, type);
                String key = String.format("%s-%s", entity.get("entityid"), eid);
                Long revisionCount = stats.get(key);
                if (revisionCount == null) {
                    stats.put(key, 0L);
                } else {
                    stats.put(key, revisionCount + 1);
                }
                //now save all revisions
                if (isPrimary) {
                    jdbcTemplate.query("SELECT eid, revisionid from janus__connectionRevision WHERE  eid = ? AND revisionid <> ?",
                        new Long[]{eid, revisionid},
                        rs2 -> {
                            saveEntity(rs.getLong("eid"), rs.getLong("revisionid"), type.concat(REVISION_POSTFIX), false, id, stats);
                        });
                }
            });
    }

    private void addMetaData(Map<String, Object> entity, Long eid, Long revisionid) {
        jdbcTemplate.query("SELECT METADATA.`key`, METADATA.`value` FROM janus__connectionRevision AS CONNECTION_REVISION " +
                "INNER JOIN janus__metadata AS METADATA ON METADATA.connectionRevisionId = CONNECTION_REVISION.id " +
                "WHERE CONNECTION_REVISION.eid = ? AND CONNECTION_REVISION.revisionid = ?",
            new Long[]{eid, revisionid},
            (rs) -> {
                parseMetaData(entity, rs.getString("key"), rs.getString("value"));
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

    private void parseMetaData(Map<String, Object> entity, String key, String value) {
        if (StringUtils.hasText(value)) {
            Map<String, String> metaDataFields = (Map<String, String>) entity.getOrDefault("metaDataFields", new HashMap<String, String>());
            metaDataFields.put(key, value);
            entity.put("metaDataFields", metaDataFields);
        }
    }

    private List<Map<String, String>> stringToNameMap(List<String> entityIds) {
        return entityIds.stream().map(s -> Collections.singletonMap("name", s)).collect(toList());
    }

}
