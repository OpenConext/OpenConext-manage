package manage.exception;


import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class ScopeDuplicateNameException extends RuntimeException {

    public ScopeDuplicateNameException(String message) {
        super(message);
    }
}
