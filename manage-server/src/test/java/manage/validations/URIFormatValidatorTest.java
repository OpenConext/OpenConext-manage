package manage.validations;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class URIFormatValidatorTest extends URLFormatValidatorTest {

    private final URIFormatValidator subject = new URIFormatValidator();

    @Test
    public void validateUri() {
        assertFalse(subject.validate("nl.uva.myuva://redirect").isPresent());
        assertFalse(subject.validate(null).isPresent());
        assertFalse(subject.validate("").isPresent());
        assertFalse(subject.validate(" ").isPresent());
        assertFalse(subject.validate("http://test").isPresent());
        assertFalse(subject.validate("https://test").isPresent());
        assertFalse(subject.validate("ftp://test-test").isPresent());
        assertFalse(subject.validate("custom:test").isPresent());
        assertFalse(subject.validate("net.dns-cloud.goalsetting://callback").isPresent());

        assertTrue(subject.validate("xxx").isPresent());
    }
}