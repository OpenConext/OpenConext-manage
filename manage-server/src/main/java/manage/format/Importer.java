package manage.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.conf.MetaDataAutoConfiguration;
import manage.hook.TypeSafetyHook;
import manage.model.EntityType;
import manage.model.MetaData;
import org.springframework.core.io.Resource;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("unchecked")
public class Importer {

    public static final String META_DATA_FIELDS = "metaDataFields";
    public static final String ARP = "arp";

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    private MetaDataFeedParser metaDataFeedParser;

    private TypeSafetyHook metaDataHook;

    public Importer(MetaDataAutoConfiguration metaDataAutoConfiguration, List<String> supportedLanguages) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataFeedParser = new MetaDataFeedParser(supportedLanguages);
        this.metaDataHook = new TypeSafetyHook(metaDataAutoConfiguration);
    }

    public Map<String, Object> importXML(Resource resource, EntityType entityType, Optional<String> entityId) throws
            IOException, XMLStreamException {
        return metaDataFeedParser.importXML(resource, entityType, entityId, metaDataAutoConfiguration);
    }

    public List<Map<String, Object>> importFeed(Resource resource) throws IOException,
            XMLStreamException {
        return metaDataFeedParser.importFeed(resource, metaDataAutoConfiguration);
    }

    public Map<String, Object> importJSON(EntityType entityType, Map<String, Object> data) throws
            JsonProcessingException {
        data.entrySet().removeIf(entry -> entry.getValue() == null);

        Map<String, Object> json = new ConcurrentHashMap<>(data);
        Object metaDataFieldsMap = json.get(META_DATA_FIELDS);
        if (metaDataFieldsMap == null || !(metaDataFieldsMap instanceof Map)) {
            metaDataAutoConfiguration.validate(json, entityType.getType());
            return Collections.EMPTY_MAP;
        }
        Map<String, Object> metaDataFields = new ConcurrentHashMap<>((Map<String, Object>) metaDataFieldsMap);
        json.put(META_DATA_FIELDS, metaDataFields);

        if (!entityType.equals(EntityType.IDP) && json.containsKey("disableConsent")) {
            json.remove("disableConsent");
        }
        if (!entityType.equals(EntityType.IDP) && json.containsKey("stepupEntities")) {
            json.remove("stepupEntities");
        }
        if (!entityType.equals(EntityType.IDP) && json.containsKey("mfaEntities")) {
            json.remove("mfaEntities");
        }
        if (!entityType.equals(EntityType.RP) && json.containsKey("allowedResourceServers")) {
            json.remove("allowedResourceServers");
        }

        if (metaDataFields.values().stream().anyMatch(value -> value instanceof Map)) {
            if (json.containsKey("allowedEntities")) {
                List<String> allowedEntities = (List<String>) json.get("allowedEntities");
                json.put("allowedEntities", allowedEntities.stream()
                        .map(name -> Collections.singletonMap("name", name)).collect(toList()));
            }
            if (json.containsKey("disableConsent")) {
                List<String> disableConsent = (List<String>) json.get("disableConsent");
                json.put("disableConsent", disableConsent.stream()
                        .map(name -> Collections.singletonMap("name", name)).collect(toList()));
            }

            if (json.containsKey("stepupEntities")) {
                List<String> stepupEntities = (List<String>) json.get("stepupEntities");
                json.put("stepupEntities", stepupEntities.stream()
                        .map(name -> Collections.singletonMap("name", name)).collect(toList()));
            }
            if (json.containsKey("mfaEntities")) {
                List<String> mfaEntities = (List<String>) json.get("mfaEntities");
                json.put("mfaEntities", mfaEntities.stream()
                        .map(name -> Collections.singletonMap("name", name)).collect(toList()));
            }
            //if the structure is nested then we need to flatten it
            Map<String, Object> flattened = new ConcurrentHashMap<>();
            metaDataFields.entrySet().stream().forEach(entry -> {
                Object value = entry.getValue();
                if (value instanceof String) {
                    flattened.put(entry.getKey(), value);
                }
                if (value instanceof Map) {
                    String keyPrefix = entry.getKey();
                    flatten(keyPrefix, (Map<String, Object>) value, flattened);
                }
            });
            json.put(META_DATA_FIELDS, flattened);
        }
        Exporter.excludedDataFields.forEach(excluded -> json.remove(excluded));

        MetaData metaData = metaDataHook.preValidate(new MetaData(entityType.getType(), json));
        Map<String, Object> migratedData = metaData.getData();

        metaDataAutoConfiguration.validate(migratedData, entityType.getType());

        return new TreeMap<>(migratedData);
    }

    private void flatten(String keyPrefix, Map<String, Object> value, Map<String, Object> target) {
        value.forEach((key, entryValue) -> {
            if (entryValue instanceof String) {
                target.put(keyPrefix + ":" + key, entryValue);
            }
            if (entryValue instanceof Map) {
                this.flatten(keyPrefix + ":" + key, (Map<String, Object>) entryValue, target);
            }
        });
    }

}
