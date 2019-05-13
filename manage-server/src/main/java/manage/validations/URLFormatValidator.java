package manage.validations;

import org.everit.json.schema.FormatValidator;
import org.springframework.util.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.regex.Pattern;

public class URLFormatValidator implements FormatValidator {

    private Pattern pattern = Pattern.compile("^(http|https)://(.*)$");

    @Override
    public Optional<String> validate(String subject) {
        if (StringUtils.isEmpty(subject)) {
            return Optional.empty();
        }
        try {
            new URL(subject);
            return pattern.matcher(subject).matches() ? Optional.empty() :
                    Optional.of("URL must match pattern: " + pattern.pattern());
        } catch (MalformedURLException e) {
            return Optional.of(StringUtils.hasText(e.getMessage()) ? e.getMessage() : "Invalid URL");
        }
    }

    @Override
    public String formatName() {
        return "url";
    }
}
