package manage.validations;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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