package mr.validations;

import org.everit.json.schema.FormatValidator;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Optional;

public class XMLFormatValidator implements FormatValidator {

    @Override
    public Optional<String> validate(String subject) {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        try {
            factory.newDocumentBuilder().parse(new ByteArrayInputStream(subject.getBytes()));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            return Optional.of(e.toString());
        }
        return Optional.empty();
    }

    @Override
    public String formatName() {
        return "xml";
    }

}
