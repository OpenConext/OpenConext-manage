package mr.control;

import mr.model.Validation;
import mr.validations.BooleanFormatValidator;
import mr.validations.CertificateFormatValidator;
import mr.validations.JSONFormatValidator;
import mr.validations.NumberFormatValidator;
import mr.validations.SAMLEmailFormatValidator;
import mr.validations.URLFormatValidator;
import mr.validations.XMLFormatValidator;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.internal.DateTimeFormatValidator;
import org.everit.json.schema.internal.EmailFormatValidator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ValidationController {

    private Map<String, FormatValidator> validators = new HashMap<>();

    public ValidationController() {
        validators.put("boolean", new BooleanFormatValidator());
        validators.put("certificate", new CertificateFormatValidator());
        validators.put("date-time", new DateTimeFormatValidator());
        validators.put("email", new EmailFormatValidator());
        validators.put("saml-email", new SAMLEmailFormatValidator());
//        validators.put("hostname", new HostnameFormatValidator());
//        validators.put("ipv4", new IPV4Validator());
//        validators.put("ipv6", new IPV6Validator());
        validators.put("number", new NumberFormatValidator());
        validators.put("url", new URLFormatValidator());
        validators.put("xml", new XMLFormatValidator());
        validators.put("json", new JSONFormatValidator());
    }

    @PostMapping("/client/validation")
    public boolean validation(@Validated @RequestBody Validation validation) {
        return !validators.get(validation.getType()).validate(validation.getValue()).isPresent();
    }
}
