package mr.validations;

import org.everit.json.schema.internal.EmailFormatValidator;
import org.springframework.util.StringUtils;

import java.util.Optional;

public class SAMLEmailValidator extends EmailFormatValidator {

    @Override
    public Optional<String> validate(String subject) {
        if (StringUtils.hasText(subject) && subject.toLowerCase().startsWith("mailto:")) {
            return super.validate(subject.toLowerCase().replaceFirst("mailto:", ""));
        }
        return super.validate(subject);
    }

    @Override
    public String formatName() {
        return "saml-email";
    }
}
