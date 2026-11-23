package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import manage.model.EntityType;
import manage.model.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "migrateInstitutionTypes", order = "024", author = "okke.harsta@surf.nl")
public class MigrateInstitutionTypes {

    private static final Logger LOG = LoggerFactory.getLogger(MigrateInstitutionTypes.class);

    private static final String KEY = "coin:institution_type";
    private static final String DEFAULT_VALUE = "Universiteit";

    static final Map<String, String> MAPPING = Map.ofEntries(
        Map.entry("Overig hoger onderwijs", "Universiteit"),
        Map.entry("MBO", "MBO"),
        Map.entry("Onderzoeksinstellingen", "Onderzoeksinstelling"),
        Map.entry("HBO", "Hogeschool"),
        Map.entry("Aan hoger onderwijs gelieerde instellingen", "Instelling rond hoger onderwijs"),
        Map.entry("Bibliotheken", "Bibliotheek"),
        Map.entry("Universiteiten", "Universiteit"),
        Map.entry("Musea", "Museum"),
        Map.entry("UMC", "Universitair Medisch Centrum"),
        Map.entry("STZ", "STZ-ziekenhuis"),
        Map.entry("Grote technologische instituten", "Onderzoeksinstelling"),
        Map.entry("OSP", "Instelling rond hoger onderwijs"));

    private static final List<String> VALID_VALUES = List.of(
        "MBO",
        "Onderzoeksinstelling",
        "Hogeschool",
        "Instelling rond hoger onderwijs",
        "Bibliotheek",
        "Universiteit",
        "Museum",
        "Universitair Medisch Centrum",
        "STZ-ziekenhuis");

    @Execution
    public void migrateInstitutionTypes(MongoTemplate mongoTemplate) {
        String idpCollection = EntityType.IDP.getType();
        int changed = 0;
        for (MetaData idp : mongoTemplate.findAll(MetaData.class, idpCollection)) {
            Map<String, Object> metaDataFields = (Map<String, Object>) idp.getData().get("metaDataFields");
            if (metaDataFields == null || !metaDataFields.containsKey(KEY)) {
                continue;
            }
            Object value = metaDataFields.get(KEY);
            // Empty values are left as is
            if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
                continue;
            }
            // Already migrated
            if (value instanceof String && VALID_VALUES.contains(value)) {
                continue;
            }
            String newValue = value instanceof String ? MAPPING.getOrDefault((String) value, DEFAULT_VALUE) : DEFAULT_VALUE;

            // Save previous IdP state as a revision
            MetaData idpPrevious = mongoTemplate.findById(idp.getId(), MetaData.class, idpCollection);
            idpPrevious.revision(UUID.randomUUID().toString());
            mongoTemplate.insert(idpPrevious, idpCollection + REVISION_POSTFIX);

            metaDataFields.put(KEY, newValue);
            idp.promoteToLatest("System", String.format("Migrated %s '%s' to '%s'", KEY, value, newValue));
            mongoTemplate.save(idp, idpCollection);
            LOG.info("Migrated {} '{}' to '{}' for IdP {}", KEY, value, newValue, idp.getData().get("entityid"));
            changed++;
        }
        LOG.info("Migration migrateInstitutionTypes completed: {} IdPs updated", changed);
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
