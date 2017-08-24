package mr.validations;

import org.junit.Test;

import static org.junit.Assert.*;

public class PatternFormatValidatorTest {

    private PatternFormatValidator subject = new PatternFormatValidator();

    @Test
    public void validate() throws Exception {
        assertTrue(subject.validate("*surf").isPresent());
        assertFalse(subject.validate(".*surf").isPresent());
    }

}