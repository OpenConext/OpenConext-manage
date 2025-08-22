package manage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ValueNotUniqueException extends RuntimeException {

    public ValueNotUniqueException(String message) {
        super(message);
    }

}
