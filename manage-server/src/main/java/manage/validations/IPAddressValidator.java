package manage.validations;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.everit.json.schema.FormatValidator;

import java.util.Optional;

public class IPAddressValidator implements FormatValidator {

    @Override
    public Optional<String> validate(String subject) {
        return InetAddressValidator.getInstance().isValid(subject) ? Optional.empty() : Optional.of("Not a valid InetAddress");
    }

    @Override
    public String formatName() {
        return "ip";
    }
}
