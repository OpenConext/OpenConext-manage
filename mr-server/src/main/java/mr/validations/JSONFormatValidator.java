package mr.validations;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.everit.json.schema.FormatValidator;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

public class JSONFormatValidator implements FormatValidator {

    private ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Optional<String> validate(String subject) {
        try {
            objectMapper.readValue(subject, Map.class);
            return Optional.empty();
        } catch (IOException e) {
            return Optional.of(e.getMessage());
        }
    }

    @Override
    public String formatName() {
        return "json";
    }
}
