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
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@RestController
public class ValidationController {

    private final Map<String, FormatValidator> validators;

    private PasswordGenerator passwordGenerator = new PasswordGenerator();
    private List<CharacterRule> rules;

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
        this.rules = initPasswordGeneratorRules();
    }

    private List<CharacterRule> initPasswordGeneratorRules() {
        CharacterRule lowerCaseRule = new CharacterRule(EnglishCharacterData.LowerCase);
        lowerCaseRule.setNumberOfCharacters(8);

        CharacterRule upperCaseRule = new CharacterRule(EnglishCharacterData.UpperCase);
        upperCaseRule.setNumberOfCharacters(8);

        CharacterRule digitRule = new CharacterRule(EnglishCharacterData.Digit);
        digitRule.setNumberOfCharacters(8);

        return Arrays.asList(lowerCaseRule, upperCaseRule, digitRule);
    }

    @PostMapping("/client/validation")
    public boolean validation(@Validated @RequestBody Validation validation) {
        return !validators.computeIfAbsent(validation.getType(), key -> {
            throw new IllegalArgumentException(String.format("No validation defined for %s", key));
        }).validate(validation.getValue()).isPresent();
    }

    @GetMapping(value = "/client/secret", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> secret() {
        return Collections.singletonMap("secret", passwordGenerator.generatePassword(24, rules));
    }
}
