package mr.validations;

import mr.TestUtils;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class XMLFormatValidatorTest implements TestUtils {

    private XMLFormatValidator subject = new XMLFormatValidator();

    @Test
    public void validate() throws Exception {
        assertTrue(subject.validate("nope").isPresent());
        assertFalse(subject.validate(readFile("/xml/expected_metadata_export_saml20_sp.xml")).isPresent());
    }

}