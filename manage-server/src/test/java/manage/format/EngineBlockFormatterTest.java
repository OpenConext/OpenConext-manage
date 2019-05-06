package manage.format;

import manage.TestUtils;
import manage.model.MetaData;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonMap;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class EngineBlockFormatterTest implements TestUtils {

    private EngineBlockFormatter subject = new EngineBlockFormatter();

    @Test
    @SuppressWarnings("unchecked")
    public void testParse() throws Exception {
        MetaData metaData = objectMapper.readValue(readFile("json/meta_data_detail.json"), MetaData.class);
        Map<String, Object> result = subject.parseServiceProvider(metaData);
        Map<String, Object> expected = objectMapper.readValue(readFile("push/push_results.json"), Map.class);
        assertEquals(new HashMap<>(expected).toString(), new HashMap<>(result).toString());
    }

    @Test
    public void addToResult() throws Exception {
        MetaData metaData = objectMapper.readValue(readFile("json/meta_data_detail.json"), MetaData.class);

        Map<String, Object> source = metaData.getData();
        Map<String, Object> result = new TreeMap<>();

        subject.addToResult(source, result, "entityid", of("name"));
        subject.addToResult(source, result, "metadata:certData", empty());
        subject.addToResult(source, result, "metadata:OrganizationDisplayName:nl", empty());
        subject.addToResult(source, result, "metadata:OrganizationDisplayName:en", empty());
        subject.addToResult(source, result, "metadata:coin:bogus", empty());

        assertAttribute("name", "https://teams.surfconext.nl/shibboleth", result);
        assertAttribute("metadata:certData",
                "MIIDEzCCAfugAwIBAgIJAKoK" +
                        "/heBjcOYMA0GCSqGSIb3DQEBBQUAMCAxHjAcBgNVBAoMFU9yZ2FuaXphdGlvbiwgQ049T0lEQzAeFw0xNTExMTExMDEyMTVaFw0yNTExMTAxMDEyMTVaMCAxHjAcBgNVBAoMFU9yZ2FuaXphdGlvbiwgQ049T0lEQzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBANBGwJ/qpTQNiSgUglSE2UzEkUow+wS8r67etxoEhlzJZfgK/k5TfG1wICDqapHAxEVgUM10aBHRctNocA5wmlHtxdidhzRZroqHwpKy2BmsKX5Z2oK25RLpsyusB1KroemgA/CjUnI6rIL1xxFn3KyOFh1ZBLUQtKNQeMS7HFGgSDAp+sXuTFujz12LFDugX0T0KB5a1+0l8y0PEa0yGa1oi6seONx849ZHxM0PRvUunWkuTM+foZ0jZpFapXe02yWMqhc/2iYMieE/3GvOguJchJt6R+cut8VBb6ubKUIGK7pmoq/TB6DVXpvsHqsDJXechxcicu4pdKVDHSec850CAwEAAaNQME4wHQYDVR0OBBYEFK7RqjoodSYVXGTVEdLf3kJflP/sMB8GA1UdIwQYMBaAFK7RqjoodSYVXGTVEdLf3kJflP/sMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEFBQADggEBADNZkxlFXh4F45muCbnQd+WmaXlGvb9tkUyAIxVL8AIu8J18F420vpnGpoUAE+Hy3evBmp2nkrFAgmr055fAjpHeZFgDZBAPCwYd3TNMDeSyMta3Ka+oS7GRFDePkMEm+kH4/rITNKUF1sOvWBTSowk9TudEDyFqgGntcdu/l/zRxvx33y3LMG5USD0x4X4IKjRrRN1BbcKgi8dq10C3jdqNancTuPoqT3WWzRvVtB/q34B7F74/6JzgEoOCEHufBMp4ZFu54P0yEGtWfTwTzuoZobrChVVBt4w/XZagrRtUCDNwRpHNbpjxYudbqLqpi1MQpV9oht/BpTHVJG2i0ro=", result);
        assertAttribute("metadata:OrganizationDisplayName:nl", "SURFnet", result);
        assertAttribute("metadata:OrganizationDisplayName:en", "SURFnet", result);
        assertAttribute("metadata:OrganizationDisplayName:bogus", null, result);
    }

    @Test
    public void emptySource() {
        MetaData metaDataContainer = new MetaData("oidc10_rp", singletonMap("metaDataFields", new HashMap<String, Object>()));
        Map<String, Object> rp = subject.parseOidcClient(metaDataContainer);
        assertEquals(Stream.of("allow_all_entities", "allowed_connections", "arp_attributes", "type").collect(Collectors.toSet()), rp.keySet());
    }

    private void assertAttribute(String path, String value, Map<String, Object> result) {
        String[] split = path.split(":");
        if (split.length == 1) {
            assertEquals(value, result.get(split[0]));
        } else {
            Iterator<String> iterator = Arrays.asList(split).iterator();
            while (iterator.hasNext()) {
                Object o = result.get(iterator.next());
                if (!iterator.hasNext()) {
                    assertEquals(value, o);
                } else {
                    result = (Map<String, Object>) o;
                }
            }
        }
    }

}