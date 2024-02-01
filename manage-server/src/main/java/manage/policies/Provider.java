package manage.policies;

import lombok.Getter;
import manage.model.EntityType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;

@Getter
@SuppressWarnings("unchecked")
public class Provider {

    private final String entityId;
    private final String institutionId;
    private final boolean allowedAll;
    private final List<String> allowedEntityIds;
    private final EntityType entityType;

    public Provider(EntityType entityType, Map<String, Object> metaData) {
        this.entityType = entityType;
        Map<String, Object> data = (Map<String, Object>) metaData.get("data");
        this.entityId = (String) data.get("entityid");
        Map<String, Object> metaDataFields = (Map<String, Object>) data.get("metaDataFields");
        this.institutionId = (String) metaDataFields.get("coin:institution_id");
        this.allowedAll = (boolean) metaData.getOrDefault("allowedall", false);
        this.allowedEntityIds = (List<String>) metaData.getOrDefault("allowedEntities", new ArrayList<>());
    }

    public boolean isAllowedFrom(String... entityIds) {
        return allowedAll || stream(entityIds).anyMatch(allowedEntityIds::contains);
    }

}
