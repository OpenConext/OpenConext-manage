package mr.migration;

import mr.model.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class JanusMigration {

    private JdbcTemplate jdbcTemplate;
    private MongoTemplate mongoTemplate;
    private ArpDeserializer arpDeserializer = new ArpDeserializer();

    @Autowired
    public JanusMigration(@Value("${migrate_data_from_janus}") boolean migrate, DataSource dataSource, MongoTemplate mongoTemplate) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.mongoTemplate = mongoTemplate;
        if (migrate && false) {
            migrate();
        }
    }

    private void migrate() {

    }

    public void saveEntities(EntityType entityType) {
        jdbcTemplate.query("SELECT CONNECTION.id, CONNECTION.revisionNr " +
                "FROM janus__connection AS CONNECTION " +
                "INNER JOIN janus__connectionRevision AS CONNECTION_REVISION ON CONNECTION_REVISION.eid = CONNECTION.id " +
                "AND CONNECTION_REVISION.revisionid = CONNECTION.revisionNr WHERE " +
                "CONNECTION.type=?",
            new String[]{entityType.getType()},
            (rs, rowNum) -> saveEntity(rs.getLong("id"), rs.getLong("revisionNr"), entityType)
        );

    }

    private long saveEntity(Long eid, Long revisionid, EntityType entityType) {
        jdbcTemplate.queryForObject("SELECT id, eid, entityid, revisionid, state, allowedall, metadataurl" +
                "allowedall, manipulation, created, arp_attributes FROM janus__connectionRevision " +
                "WHERE  eid = ? AND revisionid = ?",
            new Long[]{eid, revisionid},
            (rs, rowNum) -> {
                Map<String, Object> entity = new LinkedHashMap<>();
                entity.put("id", rs.getLong("id"));
                entity.put("eid", rs.getLong("id"));
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
                entity.put("ip", rs.getString("ip"));
                entity.put("active", rs.getString("active").equals("yes") ? true : false);
                entity.put("arp", arpDeserializer.parseArpAttributes(rs.getString("arp_attributes")));
                entity.put("notes", rs.getString("notes"));
                addMetaData(entity, eid, revisionid);
                addAllowedEntities(entity, eid, revisionid);
               // addConsentDisabled(entity, eid, revisionid);
                return new MetaData();
            });
        return 1L;
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
        entity.put("allowedEntities", allowedEntities);
    }

    private void parseMetaData(Map<String, Object> entity, String key, String value) {
        if (StringUtils.hasText(value)) {
            entity.put(key, value);
        }
    }

}
