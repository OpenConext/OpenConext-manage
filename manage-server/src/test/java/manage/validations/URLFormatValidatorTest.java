package manage.validations;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class URLFormatValidatorTest {

    private final URLFormatValidator subject = new URLFormatValidator();

    @Test
    public void validate() {
        assertFalse(subject.validate(null).isPresent());
        assertFalse(subject.validate("").isPresent());
        assertFalse(subject.validate(" ").isPresent());
        assertFalse(subject.validate("http://test").isPresent());
        assertFalse(subject.validate("https://test").isPresent());

        assertTrue(subject.validate("file://test").isPresent());
        assertTrue(subject.validate("ftp://test").isPresent());

        assertFalse(subject.validate("data:image/png;base64,iVBO").isPresent());

    }
}