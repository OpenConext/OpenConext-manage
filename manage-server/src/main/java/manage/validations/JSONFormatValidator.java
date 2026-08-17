package manage.validations;

import org.everit.json.schema.FormatValidator;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;

public class JSONFormatValidator implements FormatValidator {

    private ObjectMapper objectMapper = new JsonMapper();

    @Override
    public Optional<String> validate(String subject) {
        try {
            objectMapper.readValue(subject, Map.class);
            return Optional.empty();
        } catch (JacksonException e) {
            return Optional.of(e.getMessage());
        }
    }

    @Override
    public String formatName() {
        return "json";
    }
}
