package mr.format;

import mr.conf.MetaDataAutoConfiguration;
import mr.migration.EntityType;
import org.springframework.core.io.Resource;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;
import static mr.format.Importer.ARP;
import static mr.format.Importer.META_DATA_FIELDS;

public class MetaDataFeedParser {

    private static final List<String> languages = Arrays.asList("en", "nl");

    public Map<String, Object> importXML(EntityType type,
                                         Resource xml,
                                         Optional<String> entityIDOptional,
                                         MetaDataAutoConfiguration metaDataAutoConfiguration) throws XMLStreamException, IOException {
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
        boolean isSp = EntityType.SP.equals(type);

        Map<String, Object> schema = metaDataAutoConfiguration.schemaRepresentation(type);
        Map<String, Object> arpAttributes = isSp ? Map.class.cast(schema.get("properties")) : new HashMap<>();
        if (isSp) {
            for (String s : Arrays.asList("arp", "properties", "attributes", "properties")) {
                arpAttributes = Map.class.cast(arpAttributes.get(s));
            }
        }
        Set<String> arpKeys = arpAttributes.keySet();

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
                            if (type.equals(EntityType.SP)) {
                                addMetaDataField(metaDataFields, reader, "Binding", "SingleLogoutService_Binding");
                                addMetaDataField(metaDataFields, reader, "Location", "SingleLogoutService_Location");
                            }
                            break;
                        case "AssertionConsumerService":
                            if (!inCorrectEntityDescriptor) {
                                break;
                            }
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
                            if (!inCorrectEntityDescriptor || !isSp) {
                                break;
                            }
                                getAttributeValue(reader, "registrationAuthority").ifPresent(authority -> {
                                    metaDataFields.put("mdrpi:RegistrationInfo", authority);
                                });
                            break;
                        }
                        case "RegistrationPolicy":
                            if (!inCorrectEntityDescriptor || !isSp) {
                                break;
                            }
                                addLanguageElement(metaDataFields, reader, "mdrpi:RegistrationPolicy");
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
                            if (inAttributeConsumingService && type.equals(EntityType.SP)) {
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
                                addMultiplicity(metaDataFields, "contacts:%s:telephoneNumber", 4, reader.getElementText());
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

    private void addArpAttribute(Map<String, Object> result, XMLStreamReader reader, Set<String> arpKeys) {
        getAttributeValue(reader, "FriendlyName").ifPresent((String friendlyName) -> {
            final Map<String, Object> arp = Map.class.cast(result.getOrDefault(ARP, new TreeMap<>()));
            arp.put("enabled", true);
            result.put(ARP, arp);

            arpKeys.stream().filter(arpKey -> arpKey.endsWith(friendlyName)).findFirst().ifPresent(arpKey -> {
                arp.put(arpKey, Arrays.asList(
                    singletonMap("source", "idp"),
                    singletonMap("value", "*")));
            });
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