package manage.validations;

import org.everit.json.schema.FormatValidator;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.Optional;

public class CertificateFormatValidator implements FormatValidator {

    private final CertificateFactory certificateFactory;

    public CertificateFormatValidator() {
        try {
            this.certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new IllegalArgumentException("X.509 not supported");
        }
    }

    @Override
    public Optional<String> validate(String certificate) {
        String wrappedCert = wrapCert(certificate);
        try {
            certificateFactory.generateCertificate(new ByteArrayInputStream(wrappedCert.getBytes()));
        } catch (CertificateException e) {
            return e.getMessage().endsWith("Invalid encoding: redundant leading 0s") ?
                    Optional.empty() : Optional.of("Invalid certificate: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public String formatName() {
        return "certificate";
    }

    private String wrapCert(String certificate) {
        return "-----BEGIN CERTIFICATE-----\n" + certificate + "\n-----END CERTIFICATE-----";
    }
}
