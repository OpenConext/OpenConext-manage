package manage.validations;

import org.everit.json.schema.FormatValidator;
import org.springframework.util.StringUtils;

import java.util.Optional;
import java.util.regex.Pattern;

public class URLFormatValidator implements FormatValidator {

    private Pattern pattern = Pattern.compile("^data:image(.*)|(http|https)://(.*)$");

    @Override
    public Optional<String> validate(String subject) {
        if (StringUtils.isEmpty(subject)) {
            return Optional.empty();
        }
        return pattern.matcher(subject).matches() ? Optional.empty() :
                Optional.of("URL must match pattern: " + pattern.pattern());
    }

    @Override
    public String formatName() {
        return "url";
    }
}
