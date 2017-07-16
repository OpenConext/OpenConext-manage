package mr.validations;

import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;

public class CertificateFormatValidatorTest {

    private CertificateFormatValidator subject = new CertificateFormatValidator();

    @Test
    public void validate() throws Exception {
        Optional<String> result = subject.validate("MIIDEzCCAfugAwIBAgIJAKoK/heBjcOYMA0GCSqGSIb3DQEBBQUAMCAxHjAcBgNVBAoMFU9yZ2FuaXphdGlvbiwgQ049T0lEQzAeFw0xNTExMTExMDEyMTVaFw0yNTExMTAxMDEyMTVaMCAxHjAcBgNVBAoMFU9yZ2FuaXphdGlvbiwgQ049T0lEQzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANBGwJ/qpTQNiSgUglSE2UzEkUow+wS8r67etxoEhlzJZfgK/k5TfG1wICDqapHAxEVgUM10aBHRctNocA5wmlHtxdidhzRZroqHwpKy2BmsKX5Z2oK25RLpsyusB1KroemgA/CjUnI6rIL1xxFn3KyOFh1ZBLUQtKNQeMS7HFGgSDAp+sXuTFujz12LFDugX0T0KB5a1+0l8y0PEa0yGa1oi6seONx849ZHxM0PRvUunWkuTM+foZ0jZpFapXe02yWMqhc/2iYMieE/3GvOguJchJt6R+cut8VBb6ubKUIGK7pmoq/TB6DVXpvsHqsDJXechxcicu4pdKVDHSec850CAwEAAaNQME4wHQYDVR0OBBYEFK7RqjoodSYVXGTVEdLf3kJflP/sMB8GA1UdIwQYMBaAFK7RqjoodSYVXGTVEdLf3kJflP/sMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBADNZkxlFXh4F45muCbnQd+WmaXlGvb9tkUyAIxVL8AIu8J18F420vpnGpoUAE+Hy3evBmp2nkrFAgmr055fAjpHeZFgDZBAPCwYd3TNMDeSyMta3Ka+oS7GRFDePkMEm+kH4/rITNKUF1sOvWBTSowk9TudEDyFqgGntcdu/l/zRxvx33y3LMG5USD0x4X4IKjRrRN1BbcKgi8dq10C3jdqNancTuPoqT3WWzRvVtB/q34B7F74/6JzgEoOCEHufBMp4ZFu54P0yEGtWfTwTzuoZobrChVVBt4w/XZagrRtUCDNwRpHNbpjxYudbqLqpi1MQpV9oht/BpTHVJG2i0ro=");
        assertFalse(result.isPresent());
    }

    @Test
    public void validateWrongCert() throws Exception {
        Optional<String> result = subject.validate("nope");
        assertTrue(result.isPresent());
    }
}