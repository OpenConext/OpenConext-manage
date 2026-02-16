package manage.exception;

import lombok.Getter;
import org.everit.json.schema.ValidationException;

import java.util.Map;

@Getter
public class CustomValidationException extends RuntimeException {

    private final ValidationException validationException;
    private final Map<String, Object> data;

    public CustomValidationException(ValidationException validationException, Map<String, Object> data) {
        this.validationException = validationException;
        this.data = data;
    }

    public CustomValidationException(ValidationException validationException) {
        this(validationException, null);
    }

    @Override
    public String getMessage() {
        String message = String.join(", ", this.validationException.getAllMessages());
        String schemaLocation = this.validationException.getSchemaLocation();
        if (schemaLocation != null && !schemaLocation.trim().isEmpty()) {
            message += " in schema " + schemaLocation;
        }
        if (data != null) {
            String entityId = (String) data.get("entityid");
            String type = (String) data.get("type");
            if (entityId != null) {
                return String.format("Validation failed for entity %s (%s): %s",
                        entityId, type != null ? type : "unknown type", message);
            }
        }
        return message;
    }

}
