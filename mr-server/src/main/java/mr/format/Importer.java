package mr.format;

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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

public class Importer {


    public static final String META_DATA_FIELDS = "metaDataFields";
    private static final List<String> languages = Arrays.asList("en", "nl");
    private static final List<String> contactTypes = Arrays.asList("technical", "support", "administrative", "billing", "other");

    public Map<String, Object> importURL(String endPoint) throws XMLStreamException, IOException {
        URL url = new URL(endPoint);
        String xml = IOUtils.toString(url.openConnection().getInputStream(), Charset.defaultCharset());
        return importXML(xml);
    }

    public Map<String, Object> importXML(String xml) throws XMLStreamException {
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
                        case "AssertionConsumerService":
                            Optional<String> bindingOpt = getAttributeValue(reader, "Binding");
                            bindingOpt.ifPresent(binding ->
                                addMultiplicity(metaDataFields, "AssertionConsumerService:%s:Binding",
                                    10, binding));
                            Optional<String> locationOpt = getAttributeValue(reader, "Location");
                            locationOpt.ifPresent(location ->
                                addMultiplicity(metaDataFields, "AssertionConsumerService:%s:Location",
                                    10, location));
                            break;
                        case "OrganizationName":
                            addLanguageElement(metaDataFields, reader, "OrganizationName");
                            break;
                        case "OrganizationDisplayName":
                            addLanguageElement(metaDataFields, reader, "OrganizationDisplayName");
                            break;
                        case "OrganizationURL":
                            addLanguageElement(metaDataFields, reader, "OrganizationDisplayName");
                            break;
                        case "ContactPerson":
                            String contactType = getAttributeValue(reader, "contactType").orElse("other");
                            addMultiplicity(metaDataFields,"contacts:%s:contactType",4, contactType);
                            inContact = true;
                            break;
                        case "GivenName":
                            if (inContact) {
                                addMultiplicity(metaDataFields,"contacts:%s:givenName",4, reader.getElementText());
                            }
                            break;
                        case "SurName":
                            if (inContact) {
                                addMultiplicity(metaDataFields,"contacts:%s:surName",4, reader.getElementText());
                            }
                            break;
                        case "EmailAddress":
                            if (inContact) {
                                addMultiplicity(metaDataFields,"contacts:%s:emailAddress",4, reader.getElementText());
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
