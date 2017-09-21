package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;

public class NumberFormatValidator implements FormatValidator {
    @Override
    public Optional<String> validate(String subject) {
        try {
            Integer.parseInt(subject);
            return Optional.empty();
        } catch (NumberFormatException e) {
            return Optional.of(String.format("%s is not a valid number", subject));
        }
    }

    @Override
    public String formatName() {
        return "number";
    }
}
