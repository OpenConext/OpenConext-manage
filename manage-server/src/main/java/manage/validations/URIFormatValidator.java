package manage.validations;

import org.everit.json.schema.FormatValidator;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.regex.Pattern;

public class URIFormatValidator implements FormatValidator {

    private final Pattern pattern = Pattern.compile("^[\\w\\.\\-]+:(\\/?\\/?)[^\\s]+$");

    @Override
    public Optional<String> validate(String subject) {
        if (!StringUtils.hasText(subject) || subject.trim().isEmpty()) {
            return Optional.empty();
        }
        try {
            new URI(subject);
            return pattern.matcher(subject).matches() ? Optional.empty() :
                    Optional.of("URL must match pattern: " + pattern.pattern());
        } catch (URISyntaxException e) {
            return Optional.of(StringUtils.hasText(e.getMessage()) ? e.getMessage() : "Invalid URI");
        }
    }

    @Override
    public String formatName() {
        return "uri";
    }
}
