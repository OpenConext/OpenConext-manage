package mr.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import mr.conf.MetaDataAutoConfiguration;
import mr.migration.EntityType;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

@SuppressWarnings("checked")
public class Importer {

    public static final String META_DATA_FIELDS = "metaDataFields";
    public static final String ARP = "arp";

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    private MetaDataFeedParser metaDataFeedParser = new MetaDataFeedParser();

    public Importer(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    //TODO remove the type and determine this on the SPSSODescriptor or IDPSSODescriptor
    public Map<String, Object> importXML(EntityType type, Resource resource, Optional<String> entityId) throws IOException, XMLStreamException {
        return metaDataFeedParser.importXML(type, resource, entityId, metaDataAutoConfiguration);
    }

    public Map<String, Object> importJSON(EntityType entityType, Map<String, Object> data) throws JsonProcessingException {
        data.entrySet().removeIf(entry-> entry.getValue() == null);

        Map<String, Object> json = new ConcurrentHashMap<>(data);
        Object metaDataFieldsMap = json.get(META_DATA_FIELDS);
        if (metaDataFieldsMap == null || !(metaDataFieldsMap instanceof Map)) {
            metaDataAutoConfiguration.validate(metaDataAutoConfiguration.getObjectMapper().writeValueAsString(json),
                entityType.getType());
            return Collections.EMPTY_MAP;
        }
        Map<String, Object> metaDataFields = new ConcurrentHashMap<>((Map<String, Object>) metaDataFieldsMap);
        json.put(META_DATA_FIELDS, metaDataFields);

        if (entityType.equals(EntityType.SP) && json.containsKey("disableConsent")) {
            json.remove("disableConsent");
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
        metaDataAutoConfiguration.validate(metaDataAutoConfiguration.getObjectMapper().writeValueAsString(json),
            entityType.getType());

        return new TreeMap<>(json);
    }

    private void flatten(String keyPrefix, Map<String, Object> value, Map<String, Object> target) {
        value.entrySet().forEach(entry -> {
            Object entryValue = entry.getValue();
            if (entryValue instanceof String) {
                target.put(keyPrefix + ":" + entry.getKey(), entryValue);
            }
            if (entryValue instanceof Map) {
                this.flatten(keyPrefix + ":" + entry.getKey(), (Map<String, Object>) entryValue, target);
            }
        });
    }

}
