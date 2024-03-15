package manage.exception;

import lombok.Getter;
import org.everit.json.schema.ValidationException;

@Getter
public class CustomValidationException extends RuntimeException {

    private final ValidationException validationException;

    public CustomValidationException(ValidationException validationException) {
        this.validationException = validationException;
    }

    @Override
    public String getMessage() {
        return String.format(
                "%s, %s",
                this.validationException.toString(),
                String.join(", ", this.validationException.getAllMessages())
        );
    }

}
