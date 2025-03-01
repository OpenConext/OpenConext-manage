package manage.validations;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BasicAuthenticationUsernameFormatValidatorTest {
    private BasicAuthenticationUsernameFormatValidator subject = new BasicAuthenticationUsernameFormatValidator();

    @Test
    public void validate() throws Exception {
        Optional<String> result = subject.validate(null);
        assertTrue(result.isPresent());

        result = subject.validate(" ");
        assertTrue(result.isPresent());

        result = subject.validate("http://test");
        assertTrue(result.isPresent());

        result = subject.validate("user");
        assertFalse(result.isPresent());
    }
}