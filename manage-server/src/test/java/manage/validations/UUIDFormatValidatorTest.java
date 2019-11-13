package manage.validations;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class UUIDFormatValidatorTest {

    private UUIDFormatValidator subject = new UUIDFormatValidator();

    @Test
    public void validate() {
        Optional<String> result = subject.validate("ad93daef-0911-e511-80d0-005056956c1a");
        assertFalse(result.isPresent());
    }
}