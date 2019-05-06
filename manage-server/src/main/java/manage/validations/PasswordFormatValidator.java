package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;

public class PasswordFormatValidator implements FormatValidator {

    @Override
    public Optional<String> validate(String subject) {
        return Optional.empty();
    }

    @Override
    public String formatName() {
        return "password";
    }
}
