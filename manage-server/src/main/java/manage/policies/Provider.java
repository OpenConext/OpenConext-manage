package manage.policies;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;

@Getter
@SuppressWarnings("unchecked")
public class Provider {

    private final String entityId;
    private final String institutionId;
    private final boolean allowedAll;
    private final List<String> allowedEntityIds;

    public Provider(Map<String, Object> metaData) {
        Map<String, Object> data = (Map<String, Object>) metaData.get("data");
        this.entityId = (String) data.get("entityid");
        this.allowedEntityIds = ((List<Map<String, String>>) data.getOrDefault("allowedEntities", new ArrayList<>()))
                .stream().map(m -> m.get("name")).collect(Collectors.toList());
        Map<String, Object> metaDataFields = (Map<String, Object>) data.get("metaDataFields");
        this.institutionId = (String) metaDataFields.get("coin:institution_id");
        this.allowedAll = (boolean) metaData.getOrDefault("allowedall", false);
    }

    public boolean isAllowedFrom(String... entityIds) {
        return this.isAllowedAll() ||
                stream(entityIds).anyMatch(entityId -> this.getAllowedEntityIds().contains(entityId));
    }

}
