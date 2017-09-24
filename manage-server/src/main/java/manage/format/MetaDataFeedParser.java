package manage.format;

import manage.conf.MetaDataAutoConfiguration;
import manage.migration.EntityType;
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
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static manage.format.Importer.ARP;
import static manage.format.Importer.META_DATA_FIELDS;

@SuppressWarnings("unchecked")
public class MetaDataFeedParser {

    private static final List<String> languages = Arrays.asList("en", "nl");
    private static final String ATTRIBUTES = "attributes";

    public Map<String, Object> importXML(Resource xml,
                                         Optional<String> entityIDOptional,
                                         MetaDataAutoConfiguration metaDataAutoConfiguration) throws
        XMLStreamException, IOException {
        //despite it's name, the XMLInputFactoryImpl is not thread safe
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLStreamReader reader = factory.createXMLStreamReader(xml.getInputStream());

        Map<String, Object> result = new TreeMap<>();
        Map<String, String> metaDataFields = new TreeMap<>();

        result.put(META_DATA_FIELDS, metaDataFields);

        boolean inKeyDescriptor = false;
        boolean inContact = true;
        boolean inCorrectEntityDescriptor = !entityIDOptional.isPresent();
        boolean inAttributeConsumingService = false;
        boolean isSp = false;

        String entityType = EntityType.IDP.getJanusDbValue();
        result.put("type", entityType);

        Set<String> arpKeys = new HashSet<>();

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "EntityDescriptor":
                            Optional<String> optional = getAttributeValue(reader, "entityID");
                            if (optional.isPresent()) {
                                if (entityIDOptional.isPresent()) {
                                    inCorrectEntityDescriptor = entityIDOptional.get().equalsIgnoreCase(optional.get());
                                }
                                if (inCorrectEntityDescriptor) {
                                    result.put("entityid", optional.get());
                                }
                            }
                            break;
                        case "SPSSODescriptor":
                            if (inCorrectEntityDescriptor) {
                                isSp = true;
                                arpKeys = arpKeys(EntityType.SP, metaDataAutoConfiguration, isSp);

                                Map<String, Object> arp = new TreeMap<>();
                                arp.put("enabled", false);
                                Map<String, Object> attributes = new TreeMap<>();

                                arp.put(ATTRIBUTES, attributes);
                                result.put(ARP, arp);

                                entityType = EntityType.SP.getJanusDbValue();
                                result.put("type", entityType);
                            }
                        case "KeyDescriptor":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            if (attributeValueMatches(reader, "use", "signing") ||
                                !getAttributeValue(reader, "use").isPresent()) {
                                inKeyDescriptor = true;
                            }
                            break;
                        case "X509Certificate":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            if (inKeyDescriptor) {
                                inKeyDescriptor = false;
                                String cert = reader.getElementText();
                                addCert(metaDataFields, cert);
                            }
                            break;
                        case "SingleLogoutService":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            if (isSp) {
                                addMetaDataField(metaDataFields, reader, "Binding", "SingleLogoutService_Binding");
                                addMetaDataField(metaDataFields, reader, "Location", "SingleLogoutService_Location");
                            }
                            break;
                        case "AssertionConsumerService":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
                            if (isSp) {
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
                            if (!inCorrectEntityDescriptor || isSp) {
                                break;
                            }
                            Optional<String> bindingOpt = getAttributeValue(reader, "Binding");
                            bindingOpt.ifPresent(binding ->
                                addMultiplicity(metaDataFields, "SingleSignOnService:%s:Binding",
                                    10, binding));
                            Optional<String> locationOpt = getAttributeValue(reader, "Location");
                            locationOpt.ifPresent(location ->
                                addMultiplicity(metaDataFields, "SingleSignOnService:%s:Location",
                                    10, location));
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
                                addArpAttribute(result, reader, arpKeys);
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
                            addMultiplicity(metaDataFields, "contacts:%s:contactType", 4, contactType);
                            inContact = true;
                            break;
                        case "GivenName":
                            if (inCorrectEntityDescriptor && inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:givenName", 4, reader.getElementText());
                            }
                            break;
                        case "SurName":
                            if (inCorrectEntityDescriptor && inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:surName", 4, reader.getElementText());
                            }
                            break;
                        case "EmailAddress":
                            if (inCorrectEntityDescriptor && inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:emailAddress", 4,
                                    reader.getElementText().replaceAll(Pattern.quote("mailto:"), ""));
                            }
                            break;
                        case "TelephoneNumber":
                            if (inCorrectEntityDescriptor && inContact) {
                                addMultiplicity(metaDataFields, "contacts:%s:telephoneNumber", 4, reader
                                    .getElementText());
                            }
                            break;

                    }
                    break;
                case END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "ContactPerson":
                            inContact = false;
                            break;
                        case "AttributeConsumingService":
                            inAttributeConsumingService = false;
                            break;
                        case "EntityDescriptor":
                            if (inCorrectEntityDescriptor) {
                                //we got what we came for
                                return result;
                            }
                    }
            }
        }
        return result;
    }

    private Set<String> arpKeys(EntityType type, MetaDataAutoConfiguration metaDataAutoConfiguration, boolean isSp) {
        Map<String, Object> schema = metaDataAutoConfiguration.schemaRepresentation(type);
        Map<String, Object> arpAttributes = isSp ? Map.class.cast(schema.get("properties")) : new HashMap<>();
        if (isSp) {
            for (String s : Arrays.asList("arp", "properties", "attributes", "properties")) {
                arpAttributes = Map.class.cast(arpAttributes.get(s));
            }
        }
        return arpAttributes.keySet();
    }

    private void addArpAttribute(Map<String, Object> result, XMLStreamReader reader, Set<String> arpKeys) {
        Optional<String> friendlyName = getAttributeValue(reader, "FriendlyName");
        Optional<String> name = getAttributeValue(reader, "Name");
        if (friendlyName.isPresent()) {
            doAddArpAttribute(result, arpKeys, friendlyName.get());
        } else if (name.isPresent()) {
            doAddArpAttribute(result, arpKeys, name.get());
        }
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