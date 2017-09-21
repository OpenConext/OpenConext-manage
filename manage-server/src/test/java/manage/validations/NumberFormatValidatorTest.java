package manage.validations;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NumberFormatValidatorTest {

    private NumberFormatValidator subject = new NumberFormatValidator();

    @Test
    public void validate() throws Exception {
        assertFalse(subject.validate("1").isPresent());
        assertTrue(subject.validate("a").isPresent());
    }

}