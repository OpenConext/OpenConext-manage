package manage.validations;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JSONFormatValidatorTest {

    private JSONFormatValidator subject = new JSONFormatValidator();

    @Test
    public void validate() throws Exception {
        assertTrue(subject.validate("nope").isPresent());
        assertFalse(subject.validate("{\"a\":[true]}").isPresent());
    }

}