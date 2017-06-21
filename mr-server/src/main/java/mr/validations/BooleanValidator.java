package mr.validations;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;

public class BooleanValidator implements FormatValidator {
    @Override
    public Optional<String> validate(String subject) {
        return "yes".equals(subject) || "no".equals(subject) ? Optional.empty() :
            Optional.of(String.format("Allowed values are %s, %s", "yes", "no"));
    }

    @Override
    public String formatName() {
        return "boolean";
    }
}
