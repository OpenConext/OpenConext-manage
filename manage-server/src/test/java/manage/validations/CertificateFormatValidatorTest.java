package manage.validations;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class CertificateFormatValidatorTest {

    private CertificateFormatValidator subject = new CertificateFormatValidator();

    @Test
    public void validate() throws Exception {
        Optional<String> result = subject.validate
                ("MIIDEzCCAfugAwIBAgIJAKoK" +
                        "/heBjcOYMA0GCSqGSIb3DQEBBQUAMCAxHjAcBgNVBAoMFU9yZ2FuaXphdGlvbiwgQ049T0lEQzAeFw0xNTExMTExMDEyMTVaFw0yNTExMTAxMDEyMTVaMCAxHjAcBgNVBAoMFU9yZ2FuaXphdGlvbiwgQ049T0lEQzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANBGwJ/qpTQNiSgUglSE2UzEkUow+wS8r67etxoEhlzJZfgK/k5TfG1wICDqapHAxEVgUM10aBHRctNocA5wmlHtxdidhzRZroqHwpKy2BmsKX5Z2oK25RLpsyusB1KroemgA/CjUnI6rIL1xxFn3KyOFh1ZBLUQtKNQeMS7HFGgSDAp+sXuTFujz12LFDugX0T0KB5a1+0l8y0PEa0yGa1oi6seONx849ZHxM0PRvUunWkuTM+foZ0jZpFapXe02yWMqhc/2iYMieE/3GvOguJchJt6R+cut8VBb6ubKUIGK7pmoq/TB6DVXpvsHqsDJXechxcicu4pdKVDHSec850CAwEAAaNQME4wHQYDVR0OBBYEFK7RqjoodSYVXGTVEdLf3kJflP/sMB8GA1UdIwQYMBaAFK7RqjoodSYVXGTVEdLf3kJflP/sMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBADNZkxlFXh4F45muCbnQd+WmaXlGvb9tkUyAIxVL8AIu8J18F420vpnGpoUAE+Hy3evBmp2nkrFAgmr055fAjpHeZFgDZBAPCwYd3TNMDeSyMta3Ka+oS7GRFDePkMEm+kH4/rITNKUF1sOvWBTSowk9TudEDyFqgGntcdu/l/zRxvx33y3LMG5USD0x4X4IKjRrRN1BbcKgi8dq10C3jdqNancTuPoqT3WWzRvVtB/q34B7F74/6JzgEoOCEHufBMp4ZFu54P0yEGtWfTwTzuoZobrChVVBt4w/XZagrRtUCDNwRpHNbpjxYudbqLqpi1MQpV9oht/BpTHVJG2i0ro=");
        assertFalse(result.isPresent());
    }

    @Test
    public void validateCert() {
        Optional<String> result = subject.validate
                ("MIIDtzCCAp+gAwIBAgIRAAhNGeKsiIOnUf2yTHvFQHcwDQYJKoZIhvcNAQELBQAwdTELMAkGA1UEBhMCTkwxEzARBgNVBAgMCk92ZXJpanNzZWwxETAPBgNVBAcMCEVuc2NoZWRlMRwwGgYDVQQKDBNVbml2ZXJzaXRlaXQgVHdlbnRlMSAwHgYDVQQDDBdVVCB0ZXN0IHdlYnNpdGUgU0FNTCBTUDAeFw0xNTEwMjcxNTUyMTRaFw0xNjEwMjYxNTUyMTRaMHUxCzAJBgNVBAYTAk5MMRMwEQYDVQQIDApPdmVyaWpzc2VsMREwDwYDVQQHDAhFbnNjaGVkZTEcMBoGA1UECgwTVW5pdmVyc2l0ZWl0IFR3ZW50ZTEgMB4GA1UEAwwXVVQgdGVzdCB3ZWJzaXRlIFNBTUwgU1AwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCeiDbKpfHztH0tdc8T5pbaz5FJH44s+4ZtOqPtlileXBCz/TDjRWJaQDnmLUp4hIZgQEpJUAB9n1Cq0vdiIexXEjj6CpW0HYm1L0o6OYQiOaLw5eIZfi892hEy4zf7YHoVDbBfIvwdMIqPd+ux+yZuNhMMToqpgJOKaqwhT7hKuHLsHV1VCDTb2DX3m9+kpwqZPj+BHDpIFgiwJ4tb/wkXMHpbFACBUoMq7qe5id/6SP/9Z16vITaE0WhyUPmHpqQl2N3AQT0Li5n8KKAYXr0Ac+QMWZUvqQVMK9BBi4cHxx1ADr4I48fTMGEd1ROmRSqMau7rCcg+I5V5yZg1k9gZAgMBAAGjQjBAMB0GA1UdDgQWBBRAEycM6IeZ5JGSmtanDAz1dc13DDAfBgNVHSMEGDAWgBRAEycM6IeZ5JGSmtanDAz1dc13DDANBgkqhkiG9w0BAQsFAAOCAQEAKQCHevEIZc/O/ixguy3+RvrGUq2vDLEhOBGC+6yMHtevkCvPoe3d6fZqeK8aVjpIwkceq/qsScIrLCstKFQL8jI3vYONE0z8BONhXB+ZZfzdGBMhx4gXr7ZIVwbmJlj6DQUTC2X4BPPR1cwKDTlnrR9EUNNVgqV2AJJn9EAS/gDfA1yEsiFvkdkwvA/haRlOg+8ChEedkjon3GXiXC1AgN1JGU+dB8D/9sADoEYJg5CZhIa6TFfIA80ouYqi4w1I5IB7Z+Co8UYCFle+y/rBC2nxfgZQbasaPtNY8ry7Qdgg1fpPwivFXMEMPKDR2O7xe3Flcb6zFeys3/wIiNllqQ==");
        assertFalse(result.isPresent());
    }

    @Test
    public void validateInvalidMigrationCert() {
        Optional<String> result = subject.validate
                ("MIICEzCB/qADAgECAgEBMAsGCSqGSIb3DQEBBTAAMCIYDzIwMTQxMDAyMTc0MjMwWhgPMjAyNDExMDIxODQyMzBaMCExHzAdBgNVBAMMFmNyZWF0aXZlcHJvZ3JhbW1pbmcuaXQwgZ0wCwYJKoZIhvcNAQEBA4GNADCBiQKBgQC69jYq4x4luNfHPrVphhm+nPvPO94Z3uiiYH2YjrcAv9Tw0gZYCNcIvpqsWwaf5pWpTnrifgdOT+nnDIWhnJV1WlsO95aFTOmUP/rB5zVGZclGnOrlZRNMFWYSbvjUcoY7j89/7VZcMvMl0ptM0BDX8YQXze8ukXNzLulVv/QFjwIDAQABMAsGCSqGSIb3DQEBBQOCAQEArwG4tgzbSutmI+2x2q2QPxxSL26XoPXY6GY7lWvAvHauAYtbN5+50aXCpq15xIGnp1dOcvlivs3Zkxc6L5OOO6HEJSBYxOdJpWOpKnubboo3xAwQ3YlY0yGrggyhsUc9CORaE3YChxZx8SbBCyVuUvgGgqnZEw1xuo46YBroay5GYejJ6J+41ejQUvDHgtE33qOXlF6AIMuQJJQsB0Gv9IRmRYdyo8iodmQpa3fwBFUbSTmQvrwlGZxlCkHIp///U8OO1hqxD6DeK4oFgO0UFIWhQqD4UE0slbQZZUUC8D55UBu2xHGSlp6NYp/dhomx2PwT/DXPnnfHpqtEpNXNVg==");
        assertEquals("Invalid certificate: Empty issuer DN not allowed in X509Certificates", result.get());
    }

    @Test
    public void validateWrongCert() {
        Optional<String> result = subject.validate("nope");
        assertTrue(result.isPresent());
    }

    @Test
    public void validateCertJavaStrictness() {
        Optional<String> result = subject.validate
                ("MIIEsTCCApmgAwIBAgIDAO7zMA0GCSqGSIb3DQEBCwUAMAAwHhcNMTgxMTA3MTQz" +
                        "ODAzWhcNMjgxMTA0MTQzODAzWjAYMRYwFAYDVQQDDA1oa3UuZWR1dnBuLm5sMIIC" +
                        "IjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKCAgEAyDkFqRDUThNSqPgiXTrLwWvv" +
                        "o+ce8t9ga2jnwh/i/c402Im3ISQbzeyz3n9RUKqM/UDgSVJiSrYwvDlapw/8sN9t" +
                        "TOa4fYwrCj7xT7i4VLnmZpzk71PzObXcTtbnz2YndmpaBaJXs1na6MyrBZZ46jGK" +
                        "dOv0tMjTjAnqc3LsfX08CzVLy0Ek9bScMlBCGCW6POjX9fxgZxUXrVH9RLqYeKV6" +
                        "EU/SYv4RrZ9kx3gm/CWrMFstV87g3Bl3Kx1MLyQ3HOpbjObGUT0fRXUQV1FPdNoj" +
                        "IKTCbd4P51GX1da43578NRr4dAof+iuL+fORlP+ONiQITpZJwPdN/v87eUPc3UOB" +
                        "+Rdbdge0ei5nXN5X1KOGYp4Zi75A089EQfecBTTDCmlCBhEykFkloX3UA/NcAs6M" +
                        "Y9eqWvpE23fFOS6z/Tzq7Tmk20FQQdC24HLuqfLLApo0gMCpq9egUrzgfoWXKc2S" +
                        "5Z5sc+6eUvkih2xdGPNTOI2eanidxdCYCNMpHFtPX6aRyoeG8QiCppEauYV64gbH" +
                        "4cqVJQq2fSjWLqav93ZRZnZweFEDtVAlrIr+H7nVRfBsZm4a71r4RO6V6Tvs71Q7" +
                        "uUMLo2GEMd2mRtKMWtn5AUwpSoeSZ9bDvgXn8084foUHDhXUzu+Pr6/c9KXr1j6z" +
                        "t5TEAgXD+mZ6FF8+SzkCAwEAAaMcMBowGAYDVR0RBBEwD4INaGt1LmVkdXZwbi5u" +
                        "bDANBgkqhkiG9w0BAQsFAAOCAgEACtt/KzCUrTuFUnfBQvlyubO5xgn3KxN16vJP" +
                        "yvfmZD46eIvutXmh2Ua7np2g3tdVBUcEP7+1hZOxoNCLR2BvMa/7ZE4N3UI9ViPU" +
                        "PzOZtZ2VcXGlfKG5de8dYthOvHEPazHA3QoXgfCsOqvCJbNFF/tP4Spj/OQIgd6N" +
                        "wGhJonhUIKE14XJTpZ31fu29uZhOEiDoq0S8dSt1NCNfK5IuD9O7rCBvgH+E2CdR" +
                        "aHbGmPHJNU0ArGiuK3Bbx9LiKQsY7kzrELCFGnaEE3tHpi87d8hmwqpOh4IwQriL" +
                        "gpN/OmJSa8tgadbo980cxmKKBoq/9Cc3i1xHqpQcjdEN6W6pIho7vj1aOOSXhRR2" +
                        "wzdiHhy0LbYAUFJ6pCd+MGXy7fWukzTeDWkKtOVDa2uzqT4QeehwtJ1+c1GjG3tO" +
                        "E+L35WWIK7HQx/bHMCirYX58Lf4OLNDMlzBIB5R6slfGI1nbLBN7DG03JvKHjfaL" +
                        "Jv9rcgFdCRjeuFynPxzfo5mE2U+MXW59fWKl20BTGdnXhsEDYVu8uUqc0FbvRtNJ" +
                        "lHRcEN4TuIrmjfxMvKr2lEWM0aoTxm39uzJ5wkDjd58LY7mwdbMkleWcZoXrl0aN" +
                        "1E2MG8xXk9vCIwiDFUYej4tqqxbCTvyriao9fyTvDIl0tqNMnJhpQEgIw/cQWYZt" +
                        "MV9V3JI=");
        assertTrue(result.isPresent());
        assertEquals("Invalid certificate: Empty issuer DN not allowed in X509Certificates", result.get());
    }

}