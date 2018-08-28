package manage.model;

import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@EqualsAndHashCode
@SuppressWarnings("unchecked")
public class ProviderIdentifier {
    //Every revision that has a different entityID, state or institution_id is considered a different entity
    private String entityId;
    private String state;
    private String institutionId;

    public ProviderIdentifier(Map provider) {
        Map data = Map.class.cast(provider.get("data"));
        this.entityId = String.class.cast(data.get("entityid"));
        this.institutionId = (String) Map.class.cast(data.getOrDefault("metaDataFields", new HashMap<>())).get
            ("coin:institution_id");
        this.state = String.class.cast(data.get("state"));
    }


    public Map toMap(EntityType entityType, Optional<Map<String, String>> optionalMap) {
        Map<String, String> result = optionalMap.orElseGet(HashMap::new);
        result.put(entityType.getType() + "_entityid", entityId);
        result.put(entityType.getType() + "_state", state);
        result.put(entityType.getType() + "_coin:institution_id", institutionId);
        return result;
    }

}
