package mr.control;

import mr.model.Validation;
import mr.validations.BooleanFormatValidator;
import mr.validations.CertificateFormatValidator;
import mr.validations.JSONFormatValidator;
import mr.validations.NumberFormatValidator;
import mr.validations.PatternFormatValidator;
import mr.validations.URLFormatValidator;
import mr.validations.UUIDFormatValidator;
import mr.validations.XMLFormatValidator;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.internal.DateTimeFormatValidator;
import org.everit.json.schema.internal.EmailFormatValidator;
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
            new EmailFormatValidator(),
            new NumberFormatValidator(),
            new URLFormatValidator(),
            new XMLFormatValidator(),
            new JSONFormatValidator(),
            new UUIDFormatValidator(),
            new PatternFormatValidator()).stream().collect(toMap(FormatValidator::formatName, Function.identity()));
    }

    @PostMapping("/client/validation")
    public boolean validation(@Validated @RequestBody Validation validation) {
        return !validators.computeIfAbsent(validation.getType(), key -> {
            throw new IllegalArgumentException(String.format("No validation defined for %s", key));
        }).validate(validation.getValue()).isPresent();
    }
}
