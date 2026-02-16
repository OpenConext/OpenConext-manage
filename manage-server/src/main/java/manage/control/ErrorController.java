package manage.control;

import jakarta.servlet.http.HttpServletRequest;
import manage.exception.CustomValidationException;
import manage.exception.EndpointNotAllowed;
import manage.exception.ScopeInUseException;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.Map;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestController
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

    private static final Logger LOG = LoggerFactory.getLogger(ErrorController.class);

    private final ErrorAttributes errorAttributes;

    public ErrorController(ErrorAttributes errorAttributes) {
        this.errorAttributes = errorAttributes;
    }

    public ErrorController() {
        this(new DefaultErrorAttributes());
    }

    @RequestMapping("/error")
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        ServletWebRequest webRequest = new ServletWebRequest(request);

        Map<String, Object> result = errorAttributes.getErrorAttributes(webRequest, ErrorAttributeOptions.defaults());

        Throwable error = errorAttributes.getError(webRequest);

        if (error != null) {
            LOG.warn("Error occurred: {}", error.getMessage());
        }

        //Determine which status to return - GUI expects 200 and other API clients normal behaviour
        boolean isInternalCall = StringUtils.hasText(request.getHeader(HttpHeaders.AUTHORIZATION));
        HttpStatus status = isInternalCall ? HttpStatus.BAD_REQUEST : HttpStatus.OK;
        if (error instanceof CustomValidationException) {
            CustomValidationException cve = (CustomValidationException) error;
            ValidationException validationException = cve.getValidationException();
            Map<String, Object> response = validationExceptionResponse(result, validationException, status, webRequest);
            if (cve.getData() != null) {
                response.put("rawInput", cve.getData());
            }
            return new ResponseEntity<>(response, status);
        }
        if (error instanceof ValidationException) {
            ValidationException validationException = ValidationException.class.cast(error);
            return new ResponseEntity<>(validationExceptionResponse(result, validationException, status, webRequest), status);
        } else if (error != null && error.getClass().getName().endsWith("InternalValidationException")) {
            return new ResponseEntity<>(internalValidationExceptionResponse(result, error, status, webRequest), status);
        } else if (error instanceof OptimisticLockingFailureException) {
            String message = error.getMessage();
            result.put("validations", String.format("Optimistic locking failure: %s. " +
                    "Refresh your screen to get the latest version.", message));
            result.put("status", status.value());
            result.put("error", OptimisticLockingFailureException.class.getName());
            return new ResponseEntity<>(result, status);
        } else if (error instanceof HttpClientErrorException) {
            HttpClientErrorException httpClientErrorException = (HttpClientErrorException) error;
            result.put("status", httpClientErrorException.getStatusCode().toString());
            result.put("error", HttpClientErrorException.class.getName());
            result.put("message", httpClientErrorException.getResponseBodyAsString());
            return new ResponseEntity<>(result, status);
        } else if (error instanceof ScopeInUseException) {
            result.put("message", error.getMessage());
        } else if (error instanceof EndpointNotAllowed) {
            result.put("message", error.getMessage());
        }

        HttpStatus statusCode = result.containsKey("status") ? HttpStatus.valueOf((Integer) result.get("status")) :
            INTERNAL_SERVER_ERROR;
        if (error != null) {
            //https://github.com/spring-projects/spring-boot/issues/3057
            ResponseStatus annotation = AnnotationUtils.getAnnotation(error.getClass(), ResponseStatus.class);
            statusCode = annotation != null ? annotation.value() : statusCode;
            result.put("exception", error.getClass().getCanonicalName());
        }
        return new ResponseEntity<>(result, statusCode);
    }

    private Map<String, Object> internalValidationExceptionResponse(Map<String, Object> result, Throwable error, HttpStatus status, ServletWebRequest webRequest) {
        result.put("validations", error.getMessage());
        result.put("status", status.value());
        result.put("error", error.getClass().getName());
        return result;
    }

    private Map<String, Object> validationExceptionResponse(Map<String, Object> result, ValidationException validationException, HttpStatus status, ServletWebRequest webRequest) {
        Throwable error = errorAttributes.getError(webRequest);
        String message;
        if (error instanceof CustomValidationException) {
            message = error.getMessage();
        } else {
            message = String.join(", ", validationException.getAllMessages());
            String schemaLocation = validationException.getSchemaLocation();
            if (StringUtils.hasText(schemaLocation)) {
                message += " in schema " + schemaLocation;
            }
        }

        result.put("validations", message);
        result.put("status", status.value());
        result.put("error", ValidationException.class.getName());
        return result;
    }

}
