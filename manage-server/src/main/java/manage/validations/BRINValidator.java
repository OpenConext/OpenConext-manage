package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BRINValidator implements FormatValidator {

    private static final Pattern VALID_BRIN_REGEX =
            Pattern.compile("^[A-Z0-9]{4}$");

    @Override
    public Optional<String> validate(String subject) {
        Matcher matcher = VALID_BRIN_REGEX.matcher(subject);
        if (matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(String.format("[%s] is not a valid brin format", subject));
    }

    @Override
    public String formatName() {
        return "brin";
    }
}
