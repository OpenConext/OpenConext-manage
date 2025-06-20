package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LocalEmailFormatValidator implements FormatValidator {

    private static final Pattern VALID_EMAIL_ADDRESS_REGEX =
        Pattern.compile("^[A-Z0-9._%&+-]+@[A-Z0-9.-]+\\.[A-Z]{2,16}$", Pattern.CASE_INSENSITIVE);
    private final Pattern VALID_URL_REGEX = Pattern.compile("^(http|https)://(.*)$");

    @Override
    public Optional<String> validate(String subject) {
        Matcher emailMatcher = VALID_EMAIL_ADDRESS_REGEX.matcher(subject);
        if (emailMatcher.find()) {
            return Optional.empty();
        }
        Matcher urlMatcher = VALID_URL_REGEX.matcher(subject);
        if (urlMatcher.find()) {
            return Optional.empty();
        }
        return Optional.of(String.format("[%s] is not a valid email address and not a valid URL", subject));
    }

    @Override
    public String formatName() {
        return "local-email";
    }
}
