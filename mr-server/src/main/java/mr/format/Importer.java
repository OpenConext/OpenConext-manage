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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

public class Importer {


    public static final String META_DATA_FIELDS = "metaDataFields";

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

        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "EntityDescriptor":
                            getAttributeValue(reader, "entityID")
                                .ifPresent(entityID -> result.put("entityid",entityID));
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
                            locationOpt.ifPresent(binding ->
                                addMultiplicity(metaDataFields, "AssertionConsumerService:%s:Location",
                                    10, binding));
                    }
                    break;
                case END_ELEMENT:
//                    if (processRoles && reader.getLocalName().equals("Attribute")) {
//                        //we got what we wanted
//                        return null;
//                    }
            }
        }
        return result;
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
