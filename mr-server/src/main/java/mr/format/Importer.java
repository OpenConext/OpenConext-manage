package mr.format;

import com.fasterxml.jackson.core.JsonProcessingException;
import mr.conf.MetaDataAutoConfiguration;
import mr.migration.EntityType;
import org.apache.commons.io.IOUtils;

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
    private static final List<String> languages = Arrays.asList("en", "nl");

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    public Importer(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    public Map<String, Object> importXML(EntityType type, String xml) throws XMLStreamException, JsonProcessingException {
        //despite it's name, the XMLInputFactoryImpl is not thread safe
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLStreamReader reader = factory.createXMLStreamReader(new StringReader(xml));

        Map<String, Object> result = new TreeMap<>();
        Map<String, String> metaDataFields = new TreeMap<>();

        result.put(META_DATA_FIELDS, metaDataFields);

        boolean inKeyDescriptor = false;
        boolean inContact = true;

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "EntityDescriptor":
                            getAttributeValue(reader, "entityID")
                                .ifPresent(entityID -> result.put("entityid", entityID));
                            break;
                        case "KeyDescriptor":
                            if (attributeValueMatches(reader, "use", "signing") ||
                                !getAttributeValue(reader, "use").isPresent()) {
                                inKeyDescriptor = true;
                            }
                            break;
                        case "X509Certificate":
                            if (inKeyDescriptor) {
                                inKeyDescriptor = false;
                                String cert = reader.getElementText();
                                addCert(metaDataFields, cert);
                            }
                            break;
                        case "SingleLogoutService":
                            if (type.equals(EntityType.SP)) {
                                addMetaDataField(metaDataFields, reader, "Binding", "SingleLogoutService_Binding");
                                addMetaDataField(metaDataFields, reader, "Location", "SingleLogoutService_Location");
                            }
                            break;
                        case "AssertionConsumerService":
                            if (type.equals(EntityType.SP)) {
                                Optional<String> bindingOpt = getAttributeValue(reader, "Binding");
                                bindingOpt.ifPresent(binding ->
                                    addMultiplicity(metaDataFields, "AssertionConsumerService:%s:Binding",
                                        10, binding));
                                Optional<String> locationOpt = getAttributeValue(reader, "Location");
                                locationOpt.ifPresent(location ->
                                    addMultiplicity(metaDataFields, "AssertionConsumerService:%s:Location",
                                        10, location));
                                Optional<String> indexOpt = getAttributeValue(reader, "index");
                                indexOpt.ifPresent(index ->
                                    addMultiplicity(metaDataFields, "AssertionConsumerService:%s:index",
                                        10, index));
                            }
                            break;
                        case "SingleSignOnService":
                            if (type.equals(EntityType.IDP)) {
                                Optional<String> bindingOpt = getAttributeValue(reader, "Binding");
                                bindingOpt.ifPresent(binding ->
                                    addMultiplicity(metaDataFields, "SingleSignOnService:%s:Binding",
                                        10, binding));
                                Optional<String> locationOpt = getAttributeValue(reader, "Location");
                                locationOpt.ifPresent(location ->
                                    addMultiplicity(metaDataFields, "SingleSignOnService:%s:Location",
                                        10, location));
                            }
                            break;
                        case "OrganizationName":
                            addLanguageElement(metaDataFields, reader, "OrganizationName");
                            break;
                        case "OrganizationDisplayName":
                            addLanguageElement(metaDataFields, reader, "OrganizationDisplayName");
                            break;
                        case "OrganizationURL":
                            addLanguageElement(metaDataFields, reader, "OrganizationURL");
                            break;
                        case "ContactPerson":
                            String contactType = getAttributeValue(reader, "contactType").orElse("other");
                            addMultiplicity(metaDataFields, "contacts:%s:contactType", 4, contactType);
                            inContact = true;
                            break;
                        case "GivenName":
                            if (inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:givenName", 4, reader.getElementText());
                            }
                            break;
                        case "SurName":
                            if (inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:surName", 4, reader.getElementText());
                            }
                            break;
                        case "EmailAddress":
                            if (inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:emailAddress", 4, reader.getElementText());
                            }
                            break;

                    }
                    break;
                case END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "ContactPerson":
                            inContact = false;
                            break;
                    }
            }
        }
        return result;
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


    private void addMetaDataField(Map<String, String> metaDataFields, XMLStreamReader reader, String attributeName, String metaDataKey) {
        Optional<String> optional = getAttributeValue(reader, attributeName);
        optional.ifPresent(value -> metaDataFields.put(metaDataKey, value));
    }

    private void addLanguageElement(Map<String, String> metaDataFields, XMLStreamReader reader, String elementName) throws XMLStreamException {
        String language = getAttributeValue(reader, "lang").orElse("en");
        if (languages.contains(language)) {
            metaDataFields.put(String.format("%s:%s", elementName, language), reader.getElementText());
        }

    }

    private void addMultiplicity(Map<String, String> result, String format, int multiplicity, String value) {
        List<String> keys = IntStream.range(0, multiplicity).mapToObj(nbr -> String.format(format, nbr))
            .collect(toList());
        long count = result.keySet().stream().filter(key -> keys.contains(key)).count();
        if (count < keys.size()) {
            result.put(keys.get((int) count), value);
        }
    }

    private void addCert(Map<String, String> result, String cert) {
        List<String> certDataKeys = Arrays.asList("certData", "certData2", "certData3");
        long count = result.keySet().stream().filter(key -> certDataKeys.contains(key)).count();
        if (count < certDataKeys.size()) {
            result.put(certDataKeys.get((int) count), cert);
        }
    }

    private Optional<String> getAttributeValue(XMLStreamReader reader, String attributeName) {
        int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            QName qName = reader.getAttributeName(i);
            if (qName.getLocalPart().equalsIgnoreCase(attributeName)) {
                return Optional.of(reader.getAttributeValue(qName.getNamespaceURI(), qName.getLocalPart()));
            }
        }
        return Optional.empty();
    }

    private boolean attributeValueMatches(XMLStreamReader reader, String attributeName, String value) {
        int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            QName qName = reader.getAttributeName(i);
            if (qName.getLocalPart().equalsIgnoreCase(attributeName)) {
                return reader.getAttributeValue(qName.getNamespaceURI(), qName.getLocalPart()).equalsIgnoreCase(value);
            }
        }
        return false;
    }

}
