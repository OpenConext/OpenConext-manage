package manage.validations;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertFalse;

public class LocalEmailFormatValidatorTest {

    private LocalEmailFormatValidator subject = new LocalEmailFormatValidator();

    @Test
    public void testEmail() {
        Optional<String> result = subject.validate("oliver@aula.education");
        assertFalse(result.isPresent());
    }
}
