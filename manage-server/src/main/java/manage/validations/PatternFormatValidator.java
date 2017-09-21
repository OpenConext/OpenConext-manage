package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;
import java.util.regex.Pattern;

public class PatternFormatValidator implements FormatValidator {
    @Override
    public Optional<String> validate(String subject) {
        try {
            Pattern.compile(subject);
        } catch (IllegalArgumentException e) {
            return Optional.of(e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public String formatName() {
        return "pattern";
    }
}
