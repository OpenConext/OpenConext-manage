package manage.validations;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LocalEmailFormatValidatorTest {

    private final LocalEmailFormatValidator subject = new LocalEmailFormatValidator();

    @Test
    public void testEmail() {
        Optional<String> result = subject.validate("oliver@aula.education");
        assertFalse(result.isPresent());
    }

    @Test
    public void testURL() {
        Optional<String> result = subject.validate("https://surfnet.nl");
        assertFalse(result.isPresent());
    }

    @Test
    public void testInvalid() {
        Optional<String> result = subject.validate("nope");
        assertTrue(result.isPresent());
    }
}
