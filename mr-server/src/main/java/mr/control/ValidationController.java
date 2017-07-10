package mr.control;

import mr.validations.BooleanValidator;
import mr.validations.CertificateValidator;
import mr.validations.NumberValidator;
import mr.validations.SAMLEmailValidator;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.internal.DateTimeFormatValidator;
import org.everit.json.schema.internal.EmailFormatValidator;
import org.everit.json.schema.internal.URIFormatValidator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ValidationController {

    private Map<String, FormatValidator> validators = new HashMap<>();

    public ValidationController() {
        validators.put("boolean", new BooleanValidator());
        validators.put("certificate", new CertificateValidator());
        validators.put("date-time", new DateTimeFormatValidator());
        validators.put("email", new EmailFormatValidator());
        validators.put("saml-email", new SAMLEmailValidator());
//        validators.put("hostname", new HostnameFormatValidator());
//        validators.put("ipv4", new IPV4Validator());
//        validators.put("ipv6", new IPV6Validator());
        validators.put("number", new NumberValidator());
        validators.put("uri", new URIFormatValidator());
    }

    @GetMapping("/client/validation/{type}")
    public boolean validation(@PathVariable("type") String type, @RequestParam("value") String value) {
        return !validators.get(type).validate(value).isPresent();
    }

}
