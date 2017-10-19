package manage.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class MetaDataAlreadyExists extends RuntimeException {

    public MetaDataAlreadyExists(String entityId) {
        super(String.format("There already exists a MetaData entry with entityId: %s", entityId));
    }
}
