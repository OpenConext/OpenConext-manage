package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;
import java.util.regex.Pattern;

public class UUIDFormatValidator implements FormatValidator {

    private static Pattern UUID_PATTERN = Pattern.compile
            ("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$", Pattern.CASE_INSENSITIVE);

    @Override
    public Optional<String> validate(String subject) {
        return UUID_PATTERN.matcher(subject).matches() ?
                Optional.empty() : Optional.of(String.format("UUID %s does not match pattern %s", subject, UUID_PATTERN
                .pattern()));
    }

    @Override
    public String formatName() {
        return "uuid";
    }

}
