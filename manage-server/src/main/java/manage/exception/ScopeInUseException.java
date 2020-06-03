package manage.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.CONFLICT)
public class ScopeInUseException extends RuntimeException {

    public ScopeInUseException(String message) {
        super(message);
    }
}
