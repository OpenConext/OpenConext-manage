package mr.format;

import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

public class EduGainFeedParser {

    private final Resource resource;

    public EduGainFeedParser(Resource resource) {
        this.resource = resource;
    }

    public List<String> /*Map<String, String>*/ parse() throws IOException, XMLStreamException {
        //despite it's name, the XMLInputFactoryImpl is not thread safe
        XMLInputFactory factory = XMLInputFactory.newInstance();

        XMLStreamReader reader = factory.createXMLStreamReader(resource.getInputStream());

//        Map<String, String> serviceProviders = new HashMap<>();
//        String entityId = null;
//        boolean isServiceProvider = false, isSigning = false;
        List<String> result = new ArrayList<>();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case "EntityDescriptor":
//                            entityId = reader.getAttributeValue(null, "entityID");
//                            isServiceProvider = false;
                            result.add(reader.getAttributeValue(null, "entityID"));
                            break;
                        case "SPSSODescriptor":
//                            isServiceProvider = true;
                            break;
                        case "KeyDescriptor":
                            String use = reader.getAttributeValue(null, "use");
//                            isSigning = "signing".equals(use);
                            break;
                        case "X509Certificate": {
//                            if (isServiceProvider && isSigning) {
//                                addEntity(entityId, reader.getElementText(), serviceProviders);
//                            }
                        }
                    }
            }
        }
        return result;
    }

    private void addEntity(String entityId, String signature, Map<String, String> serviceProviders) {
        if (StringUtils.hasText(signature)) {
            serviceProviders.put(entityId, signature.replaceAll("\\s",""));
        }
    }

}