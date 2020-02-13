package manage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.FORBIDDEN)
public class EndpointNotAllowed extends RuntimeException {

    public EndpointNotAllowed() {
        this("FORBIDDEN");
    }

    public EndpointNotAllowed(String message) {
        super(message);
    }
}
