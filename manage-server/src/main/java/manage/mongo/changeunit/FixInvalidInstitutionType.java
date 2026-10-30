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
@ChangeUnit(id = "fixInvalidInstitutionType", order = "023", author = "okke.harsta@surf.nl")
public class FixInvalidInstitutionType {

    private static final Logger LOG = LoggerFactory.getLogger(FixInvalidInstitutionType.class);

    private static final String KEY = "coin:institution_type";
    private static final String DEFAULT_VALUE = "Overig hoger onderwijs";
    private static final List<String> VALID_VALUES = List.of(
        "Overig hoger onderwijs",
        "MBO",
        "Onderzoeksinstellingen",
        "HBO",
        "Aan hoger onderwijs gelieerde instellingen",
        "Bibliotheken",
        "Universiteiten",
        "Musea",
        "UMC",
        "STZ",
        "Grote technologische instituten",
        "OSP");

    @Execution
    public void fixInvalidInstitutionType(MongoTemplate mongoTemplate) {
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
            if (value instanceof String && VALID_VALUES.contains(value)) {
                continue;
            }
            // Save previous IdP state as a revision
            MetaData idpPrevious = mongoTemplate.findById(idp.getId(), MetaData.class, idpCollection);
            idpPrevious.revision(UUID.randomUUID().toString());
            mongoTemplate.insert(idpPrevious, idpCollection + REVISION_POSTFIX);

            metaDataFields.put(KEY, DEFAULT_VALUE);
            idp.promoteToLatest("System", String.format("Replaced invalid %s '%s' with '%s'", KEY, value, DEFAULT_VALUE));
            mongoTemplate.save(idp, idpCollection);
            LOG.info("Replaced invalid {} '{}' for IdP {}", KEY, value, idp.getData().get("entityid"));
            changed++;
        }
        LOG.info("Migration fixInvalidInstitutionType completed: {} IdPs updated", changed);
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
