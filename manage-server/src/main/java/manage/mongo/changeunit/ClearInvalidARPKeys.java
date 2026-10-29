package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import manage.model.EntityType;
import manage.model.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "clearInvalidARPKeys", order = "014", author = "okke.harsta@surf.nl")
public class ClearInvalidARPKeys {

    private static final Logger LOG = LoggerFactory.getLogger(ClearInvalidARPKeys.class);

    @Execution
    public void clearInvalidARPKeys(MongoTemplate mongoTemplate) {
        final List<String> allowedKeys = List.of("source", "value", "motivation", "release_as", "use_as_nameid");
        List.of(EntityType.RP, EntityType.SP, EntityType.STT).forEach(entityType -> {
            List<MetaData> metaDataList = mongoTemplate.findAll(MetaData.class, entityType.getType());
            metaDataList.forEach(metaData -> {
                if (metaData.getData().containsKey("arp")) {
                    Map<String, Object> arp = (Map<String, Object>) metaData.getData().get("arp");
                    if (!CollectionUtils.isEmpty(arp)) {
                        Object attributesObject = arp.get("attributes");
                        if (attributesObject instanceof List) {
                            arp.put("attributes", new HashMap<>());
                            LOG.info(String.format("Saving %s metadata type %s where ARP attributes is a List",
                                metaData.getData().get("entityid"), entityType));
                            mongoTemplate.save(metaData, entityType.getType());
                        } else if (attributesObject instanceof Map) {
                            Map<String, List<Map<String, Object>>> attributes = (Map<String, List<Map<String, Object>>>) arp.get("attributes");
                            if (!CollectionUtils.isEmpty(attributes)) {
                                AtomicBoolean needsSaving = new AtomicBoolean(false);
                                attributes.forEach((arpName, value) -> value.forEach(attribute -> {
                                    boolean useAsNameId = (boolean) attribute.getOrDefault("use_as_nameid",
                                        attribute.getOrDefault("use_as_name_id",
                                            attribute.getOrDefault("useAsNameId", false)));
                                    attribute.put("use_as_nameid", useAsNameId);
                                    String releaseAt = (String) attribute.getOrDefault("releaseAs", attribute.get("release_as"));
                                    if (StringUtils.hasText(releaseAt)) {
                                        attribute.put("release_as", releaseAt);
                                    }
                                    if (attribute.keySet().removeIf(key -> !allowedKeys.contains(key))) {
                                        needsSaving.set(true);
                                    }
                                }));
                                if (needsSaving.get()) {
                                    //We don't make a new revision as this is a cleanup action
                                    LOG.info(String.format("Saving %s metadata type %s where ARP values are fixed",
                                        metaData.getData().get("entityid"), entityType.getType()));
                                    mongoTemplate.save(metaData, entityType.getType());

                                }
                            }
                        } else {
                            LOG.info(String.format("Unknown attributes type in metadata %s type %s",
                                metaData.getData().get("entityid"), entityType));
                        }
                    }
                }
            });
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
