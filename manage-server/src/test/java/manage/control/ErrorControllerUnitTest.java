package manage.control;

import jakarta.servlet.http.HttpServletRequest;
import manage.exception.CustomValidationException;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.ServletWebRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ErrorControllerUnitTest {

    private ErrorController subject;
    private ErrorAttributes errorAttributes;
    private HttpServletRequest request;

    @BeforeEach
    public void before() {
        errorAttributes = mock(ErrorAttributes.class);
        subject = new ErrorController(errorAttributes);
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void errorValidationException() {
        ValidationException validationException = new ValidationException(mock(Schema.class), "Violated", "message");
        // schemaLocation is private but can be set via constructor or mocked if it's not final
        // In Everit, it's often derived from the schema.
        Map<String, Object> errorAttributesMap = new HashMap<>();
        errorAttributesMap.put("status", 500);

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
                .thenReturn(errorAttributesMap);
        when(errorAttributes.getError(any(ServletWebRequest.class))).thenReturn(validationException);

        ResponseEntity<Map<String, Object>> response = subject.error(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(((String)response.getBody().get("validations")).contains("#: Violated"));
    }

    @Test
    public void errorCustomValidationException() {
        ValidationException validationException = new ValidationException(mock(Schema.class), "Violated", "message");
        Map<String, Object> data = Map.of("key", "value", "entityid", "entity-id", "type", "saml20_sp");
        CustomValidationException customValidationException = new CustomValidationException(validationException, data);
        Map<String, Object> errorAttributesMap = new HashMap<>();
        errorAttributesMap.put("status", 500);

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
                .thenReturn(errorAttributesMap);
        when(errorAttributes.getError(any(ServletWebRequest.class))).thenReturn(customValidationException);

        ResponseEntity<Map<String, Object>> response = subject.error(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String validations = (String) response.getBody().get("validations");
        assertTrue(validations.contains("Validation failed for entity entity-id (saml20_sp)"));
        assertTrue(validations.contains("#: Violated"));
        assertEquals(data, response.getBody().get("rawInput"));
    }

    @Test
    public void errorValidationExceptionWithSchemaLocation() {
        ValidationException validationException = mock(ValidationException.class);
        when(validationException.getAllMessages()).thenReturn(List.of("Violated"));
        when(validationException.getSchemaLocation()).thenReturn("classpath:/schema.json");

        Map<String, Object> errorAttributesMap = new HashMap<>();
        errorAttributesMap.put("status", 500);

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
                .thenReturn(errorAttributesMap);
        when(errorAttributes.getError(any(ServletWebRequest.class))).thenReturn(validationException);

        ResponseEntity<Map<String, Object>> response = subject.error(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().get("validations").toString().contains("in schema classpath:/schema.json"));
    }

    @Test
    public void errorCustomValidationExceptionWithSchemaLocation() {
        ValidationException validationException = mock(ValidationException.class);
        when(validationException.getAllMessages()).thenReturn(List.of("Violated"));
        when(validationException.getSchemaLocation()).thenReturn("classpath:/schema.json");

        Map<String, Object> data = Map.of("key", "value", "entityid", "entity-id", "type", "saml20_sp");
        CustomValidationException customValidationException = new CustomValidationException(validationException, data);
        Map<String, Object> errorAttributesMap = new HashMap<>();
        errorAttributesMap.put("status", 500);

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
                .thenReturn(errorAttributesMap);
        when(errorAttributes.getError(any(ServletWebRequest.class))).thenReturn(customValidationException);

        ResponseEntity<Map<String, Object>> response = subject.error(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String validations = (String) response.getBody().get("validations");
        assertTrue(validations.contains("Validation failed for entity entity-id (saml20_sp)"));
        assertTrue(validations.contains("Violated in schema classpath:/schema.json"));
    }

    @Test
    public void errorInternalValidationException() {
        try {
            Class<?> clazz = Class.forName("org.everit.json.schema.InternalValidationException");
            // Use the constructor with Class and Object if it's there
            // InternalValidationException(Schema violatedSchema, Class<?> expectedType, Object actualValue)
            java.lang.reflect.Constructor<?> constructor = clazz.getDeclaredConstructor(Schema.class, Class.class, Object.class);
            constructor.setAccessible(true);
            Throwable internalValidationException = (Throwable) constructor.newInstance(mock(Schema.class), String.class, 123);

            Map<String, Object> errorAttributesMap = new HashMap<>();
            errorAttributesMap.put("status", 500);

            when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
                    .thenReturn(errorAttributesMap);
            when(errorAttributes.getError(any(ServletWebRequest.class))).thenReturn(internalValidationException);

            ResponseEntity<Map<String, Object>> response = subject.error(request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            // It might be treated as ValidationException if it inherits from it and doesn't match the .endsWith check
            // if it's actually handled by internalValidationExceptionResponse, then error key will be InternalValidationException
            assertTrue(response.getBody().containsKey("validations"));
        } catch (Exception e) {
             throw new RuntimeException(e);
        }
    }

    @Test
    public void errorOptimisticLockingFailureException() {
        OptimisticLockingFailureException exception = new OptimisticLockingFailureException("Cannot save entity ea408b60... with version 18 to collection saml20_idp; Has it been modified meanwhile");
        Map<String, Object> errorAttributesMap = new HashMap<>();
        errorAttributesMap.put("status", 500);

        when(errorAttributes.getErrorAttributes(any(ServletWebRequest.class), any(ErrorAttributeOptions.class)))
                .thenReturn(errorAttributesMap);
        when(errorAttributes.getError(any(ServletWebRequest.class))).thenReturn(exception);

        ResponseEntity<Map<String, Object>> response = subject.error(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        String validations = (String) response.getBody().get("validations");
        assertTrue(validations.contains("Optimistic locking failure"));
        assertTrue(validations.contains("Has it been modified meanwhile"));
        assertTrue(validations.contains("Refresh your screen"));
    }
}
