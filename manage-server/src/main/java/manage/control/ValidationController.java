package manage.control;

import manage.model.Validation;
import manage.policies.IPAddressProvider;
import manage.policies.IPInfo;
import manage.validations.*;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.internal.DateTimeFormatValidator;


import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

@RestController
public class ValidationController {

    private final Map<String, FormatValidator> validators;

    private final PasswordGenerator passwordGenerator = new PasswordGenerator();
    private final List<CharacterRule> rules;

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
                new BasicAuthenticationUsernameFormatValidator(),
                new IPAddressValidator(),
                new URIFormatValidator())
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/client/validation")
    public boolean validation(@Validated @RequestBody Validation validation) {
        return validators.computeIfAbsent(validation.getType(), key -> {
            throw new IllegalArgumentException(String.format("No validation defined for %s", key));
        }).validate(validation.getValue()).isEmpty();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/client/secret", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> secret() {
        return Collections.singletonMap("secret", passwordGenerator.generatePassword(36, rules));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/client/ipinfo", produces = MediaType.APPLICATION_JSON_VALUE)
    public IPInfo ipInfo(@RequestParam(value = "ipAddress") String ipAddress,
                         @RequestParam(required = false) Integer networkPrefix) {
        if (!validation(new Validation("ip", ipAddress))) {
            return new IPInfo();
        }
        return IPAddressProvider.getIpInfo(ipAddress, networkPrefix);
    }
}
