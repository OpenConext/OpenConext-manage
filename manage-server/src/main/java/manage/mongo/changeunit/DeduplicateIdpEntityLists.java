package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import manage.model.EntityType;
import manage.model.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "deduplicateIdpEntityLists", order = "022", author = "okke.harsta@surf.nl")
public class DeduplicateIdpEntityLists {

    private static final Logger LOG = LoggerFactory.getLogger(DeduplicateIdpEntityLists.class);

    @Execution
    public void deduplicateIdpEntityLists(MongoTemplate mongoTemplate) {
        String idpCollection = EntityType.IDP.getType();
        List<String> listKeys = List.of("allowedEntities", "stepupEntities", "mfaEntities", "disableConsent");

        List<MetaData> idps = mongoTemplate.findAll(MetaData.class, idpCollection);
        int changed = 0;
        for (MetaData idp : idps) {
            List<String> deduplicatedKeys = new ArrayList<>();
            for (String key : listKeys) {
                Object value = idp.getData().get(key);
                if (!(value instanceof List)) {
                    continue;
                }
                List<Map<String, Object>> entries = (List<Map<String, Object>>) value;
                Set<Object> seenNames = new HashSet<>();
                // Keep the first occurrence of each name
                List<Map<String, Object>> deduplicated = entries.stream()
                    .filter(entry -> seenNames.add(entry.get("name")))
                    .collect(Collectors.toList());
                if (deduplicated.size() != entries.size()) {
                    idp.getData().put(key, deduplicated);
                    deduplicatedKeys.add(String.format("%s (%d removed)", key, entries.size() - deduplicated.size()));
                }
            }
            if (deduplicatedKeys.isEmpty()) {
                continue;
            }
            // Save previous IdP state as a revision
            MetaData idpPrevious = mongoTemplate.findById(idp.getId(), MetaData.class, idpCollection);
            idpPrevious.revision(UUID.randomUUID().toString());
            mongoTemplate.insert(idpPrevious, idpCollection + REVISION_POSTFIX);

            // Promote live IdP record
            String revisionNote = "Removed duplicate entries from: " + String.join(", ", deduplicatedKeys);
            idp.promoteToLatest("System", revisionNote);
            mongoTemplate.save(idp, idpCollection);
            LOG.info("Deduplicated IdP {}: {}", idp.getData().get("entityid"), String.join(", ", deduplicatedKeys));
            changed++;
        }
        LOG.info("Migration deduplicateIdpEntityLists completed: {} IdPs updated", changed);
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
