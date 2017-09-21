package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;

public class BooleanFormatValidator implements FormatValidator {
    @Override
    public Optional<String> validate(String subject) {
        return "1".equals(subject) || "0".equals(subject) ? Optional.empty() :
            Optional.of(String.format("Allowed values are %s, %s", "1", "0"));
    }

    @Override
    public String formatName() {
        return "boolean";
    }
}
