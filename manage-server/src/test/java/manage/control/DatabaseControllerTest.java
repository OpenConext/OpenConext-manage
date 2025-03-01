package manage.control;

import manage.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DatabaseControllerTest extends AbstractIntegrationTest {

    @Test
    @SuppressWarnings("unchecked")
    public void pushPreview() throws Exception {
        Map connections = given()
                .when()
                .get("manage/api/client/playground/pushPreview")
                .then()
                .statusCode(SC_OK)
                .extract().as(Map.class);
//        System.out.println(objectMapper.writeValueAsString(connections));
        Map expected = objectMapper.readValue(readFile("push/push.expected_connections.json"), Map.class);

        assertEquals(expected, connections);
        //ensure the Sp with "coin:imported_from_edugain": true is included
        Object importFromEdugain = ((Map) ((Map) ((Map) ((Map) connections.get("connections"))
                .get("11"))
                .get("metadata"))
                .get("coin"))
                .get("imported_from_edugain");
        assertEquals("1", importFromEdugain);

        //ensure the correct ARP is exported
        List<Map<String, Object>> arpGivenNames = (List<Map<String, Object>>) ((Map) ((Map) ((Map) connections.get("connections"))
                .get("11"))
                .get("arp_attributes"))
                .get("urn:mace:dir:attribute-def:givenName");
        Map<String, Object> arpGivenName = arpGivenNames.get(0);
        List<String> keys = arpGivenName.keySet().stream().sorted().collect(Collectors.toList());
        assertEquals(List.of("motivation", "release_as", "use_as_nameid", "value"), keys);
        assertEquals("aliasGivenName", arpGivenName.get("release_as"));
        assertEquals(true, arpGivenName.get("use_as_nameid"));

        String nameSramRP = (String) ((Map) ((Map) connections.get("connections"))
                .get("15"))
                .get("name");
        assertEquals("https://sram.service.api.oidc_rp", nameSramRP);

        String nameSramSP = (String) ((Map) ((Map) connections.get("connections"))
                .get("16"))
                .get("name");
        assertEquals("https://sram.service.api.saml_sp", nameSramSP);
    }

}
