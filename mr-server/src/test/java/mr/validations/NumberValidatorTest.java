package mr.validations;

import org.junit.Test;

import static org.junit.Assert.*;

public class NumberValidatorTest {

    private NumberValidator subject = new NumberValidator();

    @Test
    public void validate() throws Exception {
        assertFalse(subject.validate("1").isPresent());
        assertTrue(subject.validate("a").isPresent());
    }

}