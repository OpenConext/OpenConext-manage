package manage.control;

import com.github.tomakehurst.wiremock.WireMockServer;
import manage.AbstractIntegrationTest;
import manage.model.PushOptions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

@TestPropertySource(properties = {
    "push.skip_in_dev=false",
    "push.pdp.url=http://localhost:9898/pdp/api/manage/push"
})
public class DatabaseControllerTest extends AbstractIntegrationTest {

    private static final WireMockServer wireMockServer = new WireMockServer(9898);

    @BeforeAll
    static void startWireMock() {
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void pushPreview() throws Exception {
        Map connections = given()
                .when()
                .get("manage/api/client/playground/pushPreview")
                .then()
                .statusCode(SC_OK)
                .extract().as(Map.class);
        //System.out.println(objectMapper.writeValueAsString(connections));
        Map expected = objectMapper.readValue(readFile("push/push.expected_connections.json"), Map.class);

        assertEquals(expected, connections);
        //ensure the Sp with "coin:imported_from_edugain": true is included
        Object importFromEdugain = ((Map) ((Map) ((Map) ((Map) connections.get("connections"))
                .get("11"))
                .get("metadata"))
                .get("coin"))
                .get("imported_from_edugain");
        assertEquals("0", importFromEdugain);

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

        Map sramService = (Map) ((Map) connections.get("connections"))
                .get("15");
        String nameSramRP = (String) sramService
                .get("name");
        assertEquals("https://sram.service.api.oidc_rp", nameSramRP);
        Map<String, Object> coinAttributes = (Map<String, Object>) ((Map) sramService.get("metadata")).get("coin");
        assertEquals("1", coinAttributes.get("collab_enabled"));

        String nameSramSP = (String) ((Map) ((Map) connections.get("connections"))
                .get("16"))
                .get("name");
        assertEquals("https://sram.service.api.saml_sp", nameSramSP);
    }

    @Test
    public void pushSuccess() {
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlEqualTo("/api/connections")).willReturn(okJson("{}")));
        wireMockServer.stubFor(put(urlEqualTo("/pdp/api/manage/push")).willReturn(aResponse().withStatus(200)));

        given()
            .contentType("application/json")
            .body(new PushOptions(true, false, true))
            .when()
            .put("manage/api/client/playground/push")
            .then()
            .statusCode(SC_OK)
            .body("eb.status", is("OK"))
            .body("pdp.status", is("OK"));

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/connections")));
        wireMockServer.verify(1, putRequestedFor(urlEqualTo("/pdp/api/manage/push")));
    }

    @Test
    public void pushInternalSuccessIncludesOidc() {
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlEqualTo("/api/connections")).willReturn(okJson("{}")));
        wireMockServer.stubFor(post(urlEqualTo("/manage/connections")).willReturn(aResponse().withStatus(200)));

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .get("manage/api/internal/push")
            .then()
            .statusCode(SC_OK)
            .body("eb.status", is("OK"))
            .body("oidc.status", is("OK"))
            .body("pdp.status", is("OK"));

        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/connections")));
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/manage/connections")));
    }

    @Test
    public void pushEbError() {
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlEqualTo("/api/connections"))
            .willReturn(aResponse().withStatus(502).withBody("{\"error\":\"eb error\"}")));
        wireMockServer.stubFor(post(urlEqualTo("/manage/connections")).willReturn(aResponse().withStatus(200)));
        wireMockServer.stubFor(put(urlEqualTo("/pdp/api/manage/push")).willReturn(aResponse().withStatus(200)));

        given()
            .contentType("application/json")
            .body(new PushOptions(true, true, true))
            .when()
            .put("manage/api/client/playground/push")
            .then()
            .statusCode(SC_INTERNAL_SERVER_ERROR)
            .body("message", containsString("Error in push to EngineBlock"));
    }

    @Test
    public void pushOidcError() {
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlEqualTo("/api/connections")).willReturn(okJson("{}")));
        wireMockServer.stubFor(post(urlEqualTo("/manage/connections"))
            .willReturn(aResponse().withStatus(503).withBody("{\"error\":\"oidc error\"}")));

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .get("manage/api/internal/push")
            .then()
            .statusCode(SC_INTERNAL_SERVER_ERROR)
            .body("message", containsString("Error in push to OIDC"));
    }

    @Test
    public void pushPdpError() {
        wireMockServer.resetAll();
        wireMockServer.stubFor(post(urlEqualTo("/api/connections")).willReturn(okJson("{}")));
        wireMockServer.stubFor(post(urlEqualTo("/manage/connections")).willReturn(aResponse().withStatus(200)));
        wireMockServer.stubFor(put(urlEqualTo("/pdp/api/manage/push"))
            .willReturn(aResponse().withStatus(500).withBody("{\"error\":\"pdp error\"}")));

        given()
            .contentType("application/json")
            .body(new PushOptions(true, true, true))
            .when()
            .put("manage/api/client/playground/push")
            .then()
            .statusCode(SC_INTERNAL_SERVER_ERROR)
            .body("message", containsString("Error in push to PDP"));
    }
}
