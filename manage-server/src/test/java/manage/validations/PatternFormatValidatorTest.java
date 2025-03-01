package manage.validations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PatternFormatValidatorTest {

    private PatternFormatValidator subject = new PatternFormatValidator();

    @Test
    public void validate() throws Exception {
        assertTrue(subject.validate("*surf").isPresent());
        assertFalse(subject.validate(".*surf").isPresent());
    }

}