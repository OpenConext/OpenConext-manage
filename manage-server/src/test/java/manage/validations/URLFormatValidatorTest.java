package manage.validations;

import org.junit.Test;

import static org.junit.Assert.*;

public class URLFormatValidatorTest {

    private URLFormatValidator subject = new URLFormatValidator();

    @Test
    public void validate() {
        assertFalse(subject.validate(null).isPresent());
        assertFalse(subject.validate("").isPresent());
        assertFalse(subject.validate("http://test").isPresent());
        assertFalse(subject.validate("https://test").isPresent());

        assertTrue(subject.validate(" ").isPresent());
        assertTrue(subject.validate("file://test").isPresent());
        assertTrue(subject.validate("ftp://test").isPresent());

    }
}