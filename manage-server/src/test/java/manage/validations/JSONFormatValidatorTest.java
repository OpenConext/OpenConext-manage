package manage.validations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class JSONFormatValidatorTest {

    private JSONFormatValidator subject = new JSONFormatValidator();

    @Test
    public void validate() throws Exception {
        assertTrue(subject.validate("nope").isPresent());
        assertFalse(subject.validate("{\"a\":[true]}").isPresent());
    }

}