package manage.model;


import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
@SuppressWarnings("unchecked")
public class ServiceProvider {

    private String id;
    private String entityId;
    private boolean importedFromEduGain;
    private String name;

    public ServiceProvider(Map map) {
        this.id = (String) map.get("_id");
        Map data = (Map) map.getOrDefault("data", new HashMap<>());
        this.entityId = (String) data.get("entityid");
        Map metaDataFields = (Map) data.getOrDefault("metaDataFields", new HashMap<>());
        this.name = (String) metaDataFields.get("name:en");
        this.importedFromEduGain =
            String.class.cast(metaDataFields.getOrDefault("coin:imported_from_edugain", "0")).equals("1");
    }
}
