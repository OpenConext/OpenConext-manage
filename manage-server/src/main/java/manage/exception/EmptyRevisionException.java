package manage.exception;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

public class EmptyRevisionException extends ValidationException {

    public EmptyRevisionException(Schema violatedSchema, String message, String keyword) {
        super(violatedSchema, message, keyword, null);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
