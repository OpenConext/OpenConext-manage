package manage.validations;

import manage.TestUtils;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class XMLFormatValidatorTest implements TestUtils {

    private XMLFormatValidator subject = new XMLFormatValidator();

    @Test
    public void validate() throws Exception {
        assertTrue(subject.validate("nope").isPresent());
        assertFalse(subject.validate(readFile("/xml/expected_metadata_export_saml20_sp.xml")).isPresent());
    }

}