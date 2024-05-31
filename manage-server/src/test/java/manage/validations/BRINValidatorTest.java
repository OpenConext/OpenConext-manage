package manage.validations;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BRINValidatorTest {

    private final BRINValidator subject = new BRINValidator();

    @Test
    public void validate() {
        Optional<String> result = subject.validate("OP12");
        assertFalse(result.isPresent());

        result = subject.validate("xxxx");
        assertTrue(result.isPresent());

        result = subject.validate("123");
        assertTrue(result.isPresent());
    }

}