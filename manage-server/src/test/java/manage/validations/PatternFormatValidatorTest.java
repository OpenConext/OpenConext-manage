package manage.validations;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PatternFormatValidatorTest {

    private PatternFormatValidator subject = new PatternFormatValidator();

    @Test
    public void validate() throws Exception {
        assertTrue(subject.validate("*surf").isPresent());
        assertFalse(subject.validate(".*surf").isPresent());
    }

}