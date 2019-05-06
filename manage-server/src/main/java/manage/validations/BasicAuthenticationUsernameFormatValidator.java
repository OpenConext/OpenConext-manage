package manage.validations;

import org.everit.json.schema.FormatValidator;
import org.springframework.util.StringUtils;

import java.util.Optional;

public class BasicAuthenticationUsernameFormatValidator implements FormatValidator {

    @Override
    public Optional<String> validate(String subject) {
        if (!StringUtils.hasText(subject)) {
            return Optional.of(String.format("The username in basic authentication must not be empty", subject));
        }
        if (subject.contains(":")) {
            return Optional.of(String.format("[%s] is not a valid basic authentication user", subject));
        }
        return Optional.empty();
    }

    @Override
    public String formatName() {
        return "basic-authentication-user";
    }
}
