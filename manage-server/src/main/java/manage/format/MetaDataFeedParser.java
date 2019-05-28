package manage.format;

import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import org.springframework.core.io.Resource;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static manage.format.Importer.ARP;
import static manage.format.Importer.META_DATA_FIELDS;

@SuppressWarnings("unchecked")
public class MetaDataFeedParser {

    private static final String ATTRIBUTES = "attributes";
    private List<String> languages;

    public MetaDataFeedParser(List<String> languages) {
        this.languages = languages;
    }

    public List<Map<String, Object>> importFeed(Resource xml,
                                                MetaDataAutoConfiguration metaDataAutoConfiguration) throws
            XMLStreamException, IOException {
        //despite it's name, the XMLInputFactoryImpl is not thread safe
        XMLInputFactory factory = getFactory();

        XMLStreamReader reader = factory.createXMLStreamReader(xml.getInputStream());
        List<Map<String, Object>> results = new ArrayList<>();

        while (reader.hasNext()) {
            Map<String, Object> entity = parseEntity(EntityType.SP, Optional.empty(), metaDataAutoConfiguration,
                    reader, true);
            results.add(entity);
        }
        return results;
    }

    private XMLInputFactory getFactory() {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false); // This disables DTDs entirely for that factory
        xmlInputFactory.setProperty("javax.xml.stream.isSupportingExternalEntities", false); // disable external entities
        return xmlInputFactory;
    }

    public Map<String, Object> importXML(Resource xml,
                                         EntityType entityType,
                                         Optional<String> entityIDOptional,
                                         MetaDataAutoConfiguration metaDataAutoConfiguration) throws
            XMLStreamException, IOException {
        //despite it's name, the XMLInputFactoryImpl is not thread safe
        XMLInputFactory factory = getFactory();

        XMLStreamReader reader = factory.createXMLStreamReader(xml.getInputStream());

        return parseEntity(entityType, entityIDOptional, metaDataAutoConfiguration, reader, false);
    }

    private Map<String, Object> parseEntity(EntityType entityType,
                                            Optional<String> entityIDOptional,
                                            MetaDataAutoConfiguration metaDataAutoConfiguration,
                                            XMLStreamReader reader,
                                            boolean enforceTypeStrictness) throws XMLStreamException {
        Map<String, Object> result = new TreeMap<>();
        Map<String, String> metaDataFields = new TreeMap<>();

        result.put(META_DATA_FIELDS, metaDataFields);

        boolean inKeyDescriptor = false;
        boolean inContact = true;
        boolean inUIInfo = false;
        boolean inCorrectEntityDescriptor = false;
        boolean inAttributeConsumingService = false;
        boolean inEntityAttributes = false;
        boolean typeMismatch = false;
        boolean inCorrectTypeDescriptor = false;

        result.put("type", entityType.getJanusDbValue());
        boolean isSp = entityType.equals(EntityType.SP);

        Set<String> arpKeys = new HashSet<>();
        Map<String, String> arpAliases = new HashMap<>();

        Map<String, Object> spSchema = metaDataAutoConfiguration.schemaRepresentation(EntityType.SP);

        int assertionConsumerServiceMultiplicity = getMultiplicity(spSchema, "^AssertionConsumerService:", ":index$");
        Map<String, Object> idpSchema = metaDataAutoConfiguration.schemaRepresentation(EntityType.IDP);

        int singleSignOnServiceMultiplicity = getMultiplicity(idpSchema, "^SingleSignOnService:", ":Binding$");
        int contactsMultiplicity = getMultiplicity(idpSchema, "^contacts:", ":contactType$");

        List<String> validSingleSignOnServiceBindings = getValidBindings(idpSchema, "SingleSignOnService_Binding");
        List<String> validSingleLogoutServiceBindings = getValidBindings(spSchema, "SingleLogoutService_Binding");
        List<String> validAssertionConsumerServiceBindings = getValidBindings(spSchema, "AssertionConsumerService_Binding");

        while (reader.hasNext()) {
            int next = reader.next();
            switch (next) {
                case START_ELEMENT:
                    String startLocalName = reader.getLocalName();
                    switch (startLocalName) {
                        case "EntitiesDescriptor":
                            break;
                        case "EntityDescriptor":
                            Optional<String> optional = getAttributeValue(reader, "entityID");
                            if (optional.isPresent()) {
                                if (entityIDOptional.isPresent()) {
                                    inCorrectEntityDescriptor = entityIDOptional.get().equalsIgnoreCase(optional.get());
                                }
                                if (inCorrectEntityDescriptor || !entityIDOptional.isPresent()) {
                                    result.put("entityid", optional.get());
                                }
                            }
                            break;
                        case "SPSSODescriptor":
                            if (isSp && !entityIDOptional.isPresent()) {
                                inCorrectEntityDescriptor = true;
                            }
                            if (inCorrectEntityDescriptor) {
                                inCorrectTypeDescriptor = isSp;
                                if (isSp) {
                                    arpKeys = arpKeys(EntityType.SP, metaDataAutoConfiguration, isSp);
                                    arpAliases = arpAliases(EntityType.SP, metaDataAutoConfiguration, isSp);

                                    Map<String, Object> arp = new TreeMap<>();
                                    arp.put("enabled", false);
                                    Map<String, Object> attributes = new TreeMap<>();

                                    arp.put(ATTRIBUTES, attributes);
                                    result.put(ARP, arp);
                                } else {
                                    //This should not happen, but an exception breaks reading the feed
                                    typeMismatch = true;
                                }
                            }
                            break;
                        case "IDPSSODescriptor":
                            if (!isSp && !entityIDOptional.isPresent()) {
                                inCorrectEntityDescriptor = true;
                            }
                            if (inCorrectEntityDescriptor) {
                                inCorrectTypeDescriptor = !isSp;
                                if (isSp) {
                                    //This should not happen, but an exception breaks reading the feed
                                    typeMismatch = true;
                                }
                            }
                            break;
                        case "KeyDescriptor":
                            if (!inCorrectEntityDescriptor || !inCorrectTypeDescriptor) {
                                break;
                            }
                            Optional<String> use = getAttributeValue(reader, "use");
                            if (!use.isPresent() || use.get().equals("signing")) {
                                inKeyDescriptor = true;
                            }
                            break;
                        case "X509Certificate":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            if (inKeyDescriptor) {
                                inKeyDescriptor = false;
                                String cert = reader.getElementText().replaceAll("[\n\r]", "").trim();
                                addCert(metaDataFields, cert);
                            }
                            break;
                        case "SingleLogoutService":
                            if (inCorrectEntityDescriptor && isSp) {
                                Optional<String> bindingOpt = getAttributeValue(reader, "Binding");
                                if (bindingOpt.isPresent()) {
                                    String binding = bindingOpt.get();
                                    if (validSingleLogoutServiceBindings.contains(binding)) {
                                        addMetaDataField(metaDataFields, reader, "Binding", "SingleLogoutService_Binding");
                                        addMetaDataField(metaDataFields, reader, "Location", "SingleLogoutService_Location");
                                    }
                                }
                            }
                            break;
                        case "AssertionConsumerService":
                            if (inCorrectEntityDescriptor && isSp) {
                                Optional<String> bindingOpt = getAttributeValue(reader, "Binding");
                                if (bindingOpt.isPresent()) {
                                    String binding = bindingOpt.get();
                                    if (validAssertionConsumerServiceBindings.contains(binding)) {
                                        addMultiplicity(metaDataFields, "AssertionConsumerService:%s:Binding",
                                                assertionConsumerServiceMultiplicity, binding);
                                        Optional<String> locationOpt = getAttributeValue(reader, "Location");
                                        locationOpt.ifPresent(location ->
                                                addMultiplicity(metaDataFields, "AssertionConsumerService:%s:Location",
                                                        assertionConsumerServiceMultiplicity, location));
                                        Optional<String> indexOpt = getAttributeValue(reader, "index");
                                        indexOpt.ifPresent(index ->
                                                addMultiplicity(metaDataFields, "AssertionConsumerService:%s:index",
                                                        assertionConsumerServiceMultiplicity, index));
                                    }
                                }
                            }
                            break;
                        case "SingleSignOnService":
                            if (!inCorrectEntityDescriptor || isSp) {
                                break;
                            }
                            Optional<String> bindingOpt = getAttributeValue(reader, "Binding");
                            if (bindingOpt.isPresent()) {
                                String binding = bindingOpt.get();
                                if (validSingleSignOnServiceBindings.contains(binding)) {
                                    addMultiplicity(metaDataFields, "SingleSignOnService:%s:Binding",
                                            singleSignOnServiceMultiplicity, binding);
                                    Optional<String> locationOpt = getAttributeValue(reader, "Location");
                                    locationOpt.ifPresent(location ->
                                            addMultiplicity(metaDataFields, "SingleSignOnService:%s:Location",
                                                    singleSignOnServiceMultiplicity, location));
                                }
                            }
                            break;
                        case "RegistrationInfo": {
                            if (inCorrectEntityDescriptor) {
                                getAttributeValue(reader, "registrationAuthority").ifPresent(authority -> {
                                    metaDataFields.put("mdrpi:RegistrationInfo", authority);
                                });
                            }
                            break;
                        }
                        case "RegistrationPolicy":
                            if (inCorrectEntityDescriptor) {
                                addLanguageElement(metaDataFields, reader, "mdrpi:RegistrationPolicy");
                            }
                            break;
                        case "AttributeConsumingService":
                            inAttributeConsumingService = inCorrectEntityDescriptor;
                            break;
                        case "UIInfo":
                            inUIInfo = inCorrectEntityDescriptor;
                            break;
                        case "DisplayName":
                            if (inUIInfo) {
                                addLanguageElement(metaDataFields, reader, "name");
                            }
                            break;
                        case "Logo":
                            if (inUIInfo) {
                                addLogo(metaDataFields, reader);
                            }
                            break;
                        case "Description":
                            if (inUIInfo) {
                                addLanguageElement(metaDataFields, reader, "description");
                            }
                            break;
                        case "PrivacyStatementURL":
                            if (inUIInfo) {
                                addLanguageElement(metaDataFields, reader, "mdui:PrivacyStatementURL");
                            }
                            break;
                        case "ServiceName":
                            if (inAttributeConsumingService) {
                                addLanguageElement(metaDataFields, reader, "name");
                            }
                            break;
                        case "ServiceDescription":
                            if (inAttributeConsumingService) {
                                addLanguageElement(metaDataFields, reader, "description");
                            }
                            break;
                        case "RequestedAttribute":
                            if (inAttributeConsumingService && isSp) {
                                addArpAttribute(result, reader, arpKeys, arpAliases);
                            }
                            break;
                        case "OrganizationName":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            addLanguageElement(metaDataFields, reader, "OrganizationName");
                            break;
                        case "OrganizationDisplayName":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            addLanguageElement(metaDataFields, reader, "OrganizationDisplayName");
                            break;
                        case "OrganizationURL":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            addLanguageElement(metaDataFields, reader, "OrganizationURL");
                            break;
                        case "ContactPerson":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            String contactType = getAttributeValue(reader, "contactType").orElse("other");
                            addMultiplicity(metaDataFields, "contacts:%s:contactType", contactsMultiplicity, contactType);
                            inContact = true;
                            break;
                        case "GivenName":
                            if (inCorrectEntityDescriptor && inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:givenName", contactsMultiplicity, reader.getElementText());
                            }
                            break;
                        case "SurName":
                            if (inCorrectEntityDescriptor && inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:surName", contactsMultiplicity, reader.getElementText());
                            }
                            break;
                        case "EmailAddress":
                            if (inCorrectEntityDescriptor && inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:emailAddress", contactsMultiplicity,
                                        reader.getElementText().replaceAll(Pattern.quote("mailto:"), ""));
                            }
                            break;
                        case "TelephoneNumber":
                            if (inCorrectEntityDescriptor && inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:telephoneNumber", contactsMultiplicity, reader
                                        .getElementText());
                            }
                            break;
                        case "EntityAttributes":
                            inEntityAttributes = inCorrectEntityDescriptor;
                            break;
                        case "AttributeValue":
                            if (inEntityAttributes) {
                                addCoinEntityCategories(entityType, metaDataFields, metaDataAutoConfiguration,
                                        reader.getElementText());
                            }
                            break;
                    }
                    break;
                case END_ELEMENT:
                    String localName = reader.getLocalName();
                    switch (localName) {
                        case "ContactPerson":
                            inContact = false;
                            break;
                        case "AttributeConsumingService":
                            inAttributeConsumingService = false;
                            break;
                        case "UIInfo":
                            inUIInfo = false;
                            break;
                        case "IDPSSODescriptor":
                            inCorrectTypeDescriptor = !isSp;
                            break;
                        case "SPSSODescriptor":
                            inCorrectTypeDescriptor = isSp;
                            break;
                        case "EntityDescriptor":
                            if (inCorrectEntityDescriptor) {
                                //we got what we came for
                                return typeMismatch && enforceTypeStrictness ? new HashMap<>() :
                                        this.enrichMetaData(result);
                            }
                            break;
                        case "EntityAttributes":
                            inEntityAttributes = false;
                            break;
                    }
                    break;
            }
        }
        return new HashMap<>();
    }

    private int getMultiplicity(Map<String, Object> schema, String... keyParts) {
        List<String> keyPartsList = Arrays.asList(keyParts);
        Map<String, Object> patternProperties = (Map) ((Map) ((Map) schema.get("properties")).get("metaDataFields")).get("patternProperties");
        Optional<String> optionalKey = patternProperties.keySet().stream().filter(key -> keyPartsList.stream().allMatch(part -> key.contains(part))).findAny();
        String key = optionalKey.orElseThrow(() -> new IllegalArgumentException(String.format("No key %s in schema %s", keyPartsList, patternProperties)));
        return (int) ((Map) patternProperties.get(key)).get("multiplicity");
    }

    private List<String> getValidBindings(Map<String, Object> schema, String key) {
        return (List<String>) ((Map<String, Map<String, Object>>) schema.get("definitions")).get(key).get("enum");
    }

    private Map<String, Object> enrichMetaData(Map<String, Object> metaData) {
        Map<String, String> metaDataFields = (Map<String, String>) metaData.get(META_DATA_FIELDS);
        this.languages.forEach(language -> {
            if (!metaDataFields.containsKey("name:" + language) && metaDataFields.containsKey("OrganizationName:" + language)) {
                metaDataFields.put("name:" + language, metaDataFields.get("OrganizationName:" + language));
            }
        });
        return metaData;
    }

    private Set<String> arpKeys(EntityType type, MetaDataAutoConfiguration metaDataAutoConfiguration, boolean isSp) {
        return arpAttributes(type, metaDataAutoConfiguration, isSp).keySet();
    }

    private Map<String, String> arpAliases(EntityType type, MetaDataAutoConfiguration metaDataAutoConfiguration,
                                           boolean isSp) {
        Map<String, Object> arp = arpAttributes(type, metaDataAutoConfiguration, isSp);
        return arp.entrySet().stream()
                .filter(entry -> Map.class.cast(entry.getValue()).containsKey("alias"))
                .collect(toMap(
                        entry -> (String) Map.class.cast(entry.getValue()).get("alias"),
                        entry -> entry.getKey(),
                        (alias1, alias2) -> alias1));
    }

    private Map<String, Object> arpAttributes(EntityType type, MetaDataAutoConfiguration metaDataAutoConfiguration,
                                              boolean isSp) {
        Map<String, Object> schema = metaDataAutoConfiguration.schemaRepresentation(type);
        Map<String, Object> arpAttributes = isSp ? Map.class.cast(schema.get("properties")) : new HashMap<>();
        if (isSp) {
            for (String s : Arrays.asList("arp", "properties", "attributes", "properties")) {
                arpAttributes = Map.class.cast(arpAttributes.get(s));
            }
        }
        return arpAttributes;

    }

    private void addArpAttribute(Map<String, Object> result, XMLStreamReader reader, Set<String> arpKeys,
                                 Map<String, String> arpAliases) {
        Optional<String> name = getAttributeValue(reader, "Name");
        Optional<String> friendlyName = getAttributeValue(reader, "FriendlyName");
        if (this.shouldAddAttributeToArp(name, arpKeys)) {
            doAddArpAttribute(result, arpKeys, name.get());
        } else if (this.shouldAddAttributeToArp(friendlyName, arpKeys)) {
            doAddArpAttribute(result, arpKeys, friendlyName.get());
        } else if (name.isPresent() && arpAliases.containsKey(name.get())) {
            doAddArpAttribute(result, arpKeys, arpAliases.get(name.get()));
        }
    }

    private boolean shouldAddAttributeToArp(Optional<String> name, Set<String> allowedArpKeys) {
        return name.isPresent() && allowedArpKeys.stream().anyMatch(arpKey -> arpKey.endsWith(name.get()));
    }

    private void doAddArpAttribute(Map<String, Object> result, Set<String> arpKeys, String friendlyName) {
        Map<String, Object> arp = Map.class.cast(result.getOrDefault(ARP, new TreeMap<>()));
        arp.put("enabled", true);
        final Map<String, Object> attributes = Map.class.cast(arp.getOrDefault(ATTRIBUTES, new TreeMap<>()));
        arpKeys.stream().filter(arpKey -> arpKey.endsWith(friendlyName)).findFirst().ifPresent(arpKey -> {
            List<Map<String, String>> arpEntry = List.class.cast(attributes.getOrDefault(arpKey, new ArrayList<>()));
            Map<String, String> arpValue = new HashMap<>();
            arpValue.put("source", "idp");
            arpValue.put("value", "*");
            arpEntry.add(arpValue);
            attributes.put(arpKey, arpEntry);
        });
        arp.put(ATTRIBUTES, attributes);
        result.put(ARP, arp);
    }

    private void addMetaDataField(Map<String, String> metaDataFields, XMLStreamReader reader, String attributeName,
                                  String metaDataKey) {
        Optional<String> optional = getAttributeValue(reader, attributeName);
        optional.ifPresent(value -> metaDataFields.put(metaDataKey, value));
    }

    private void addLanguageElement(Map<String, String> metaDataFields, XMLStreamReader reader, String elementName)
            throws XMLStreamException {
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
        long count = result.keySet().stream().filter(certDataKeys::contains).count();
        if (count < certDataKeys.size()) {
            // we don't want duplicates
            if (!certDataKeys.stream().map(key -> cert.equals(result.get(key))).anyMatch(b -> b)) {
                result.put(certDataKeys.get((int) count), cert);
            }

        }
    }

    private void addLogo(Map<String, String> metaDataFields, XMLStreamReader reader) throws XMLStreamException {
        getAttributeValue(reader, "width")
                .ifPresent(width -> metaDataFields.put("logo:0:width", width));
        getAttributeValue(reader, "height")
                .ifPresent(height -> metaDataFields.put("logo:0:height", height));
        metaDataFields.put("logo:0:url", reader.getElementText());
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

    private void addCoinEntityCategories(EntityType entityType, Map<String, String> metaDataFields,
                                         MetaDataAutoConfiguration metaDataAutoConfiguration, String elementText) {
        Map<String, Object> schema = metaDataAutoConfiguration.schemaRepresentation(entityType);
        Map<String, Object> schemaPart = this.unpack(schema,
                Arrays.asList("properties", "metaDataFields", "patternProperties", "^coin:entity_categories:"));
        List<String> enumeration = (List<String>) schemaPart.get("enum");
        String strippedValue = elementText.replaceAll("\n", "").trim();
        if (!enumeration.contains(strippedValue)) {
            return;
        }
        String entityCategoryKey = "coin:entity_categories:";
        //do not add the same entity category twice as we limit the number
        List<String> categories = metaDataFields.entrySet().stream()
                .filter(e -> e.getKey().startsWith(entityCategoryKey))
                .map(Map.Entry::getValue)
                .collect(toList());
        if (categories.contains(strippedValue)) {
            return;
        }

        int multiplicity = (int) schemaPart.get("multiplicity");
        int startIndex = (int) schemaPart.get("startIndex");

        metaDataFields.keySet().stream()
                .filter(s -> s.startsWith(entityCategoryKey))
                .map(s -> Integer.valueOf(s.substring(entityCategoryKey.length())))
                .max(Integer::compareTo)
                .map(i -> multiplicity >= startIndex + i ? "put returns null" + metaDataFields.put(entityCategoryKey + ++i,
                        strippedValue) : "")
                .orElseGet(() -> metaDataFields.put(entityCategoryKey + startIndex, strippedValue));
    }

    private Map<String, Object> unpack(Map<String, Object> schema, List<String> keys) {
        return keys.stream().sequential().reduce(schema, (map, s) -> s.startsWith("^") ?
                        Map.class.cast(map.get(map.keySet().stream().filter(k -> k.startsWith(s)).findAny()
                                .orElseThrow(() -> new IllegalArgumentException("Key not present: " + s)))) :
                        Map.class.cast(map.get(s)),
                (acc, com) -> com);
    }

}