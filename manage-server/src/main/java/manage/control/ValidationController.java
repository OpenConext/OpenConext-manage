package manage.control;

import manage.model.Validation;
import manage.validations.BasicAuthenticationUsernameFormatValidator;
import manage.validations.BooleanFormatValidator;
import manage.validations.CertificateFormatValidator;
import manage.validations.JSONFormatValidator;
import manage.validations.ListFormatValidator;
import manage.validations.LocalEmailFormatValidator;
import manage.validations.NoopFormatValidator;
import manage.validations.NumberFormatValidator;
import manage.validations.PasswordFormatValidator;
import manage.validations.PatternFormatValidator;
import manage.validations.URLFormatValidator;
import manage.validations.UUIDFormatValidator;
import manage.validations.XMLFormatValidator;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.internal.DateTimeFormatValidator;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@RestController
public class ValidationController {

    private final Map<String, FormatValidator> validators;

    public ValidationController() {
        this.validators = Arrays.asList(
                new BooleanFormatValidator(),
                new CertificateFormatValidator(),
                new DateTimeFormatValidator(),
                new LocalEmailFormatValidator(),
                new NumberFormatValidator(),
                new URLFormatValidator(),
                new XMLFormatValidator(),
                new JSONFormatValidator(),
                new UUIDFormatValidator(),
                new NoopFormatValidator(),
                new PatternFormatValidator(),
                new ListFormatValidator(),
                new PasswordFormatValidator(),
                new BasicAuthenticationUsernameFormatValidator())
                .stream()
                .collect(toMap(FormatValidator::formatName, Function.identity()));
    }

    @PostMapping("/client/validation")
    public boolean validation(@Validated @RequestBody Validation validation) {
        return !validators.computeIfAbsent(validation.getType(), key -> {
            throw new IllegalArgumentException(String.format("No validation defined for %s", key));
        }).validate(validation.getValue()).isPresent();
    }
}
