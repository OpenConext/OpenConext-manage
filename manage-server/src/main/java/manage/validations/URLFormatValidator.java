package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

public class URLFormatValidator implements FormatValidator {
    @Override
    public Optional<String> validate(String subject) {
        try {
            new URL(subject);
            return Optional.empty();
        } catch (MalformedURLException e) {
            return Optional.of(e.getMessage());
        }
    }

    @Override
    public String formatName() {
        return "url";
    }
}
