package manage.validations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NumberFormatValidatorTest {

    private NumberFormatValidator subject = new NumberFormatValidator();

    @Test
    public void validate() throws Exception {
        assertFalse(subject.validate("1").isPresent());
        assertTrue(subject.validate("a").isPresent());
    }

}