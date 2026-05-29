package manage.control;

import io.restassured.common.mapper.TypeRef;
import manage.AbstractIntegrationTest;
import manage.policies.PdpPolicyDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.put;
import static com.github.tomakehurst.wiremock.client.WireMock.putRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static io.restassured.http.ContentType.JSON;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("unchecked")
public class DatabaseControllerTest extends AbstractIntegrationTest {

    @Test
    public void pushToEbOidcAndPdp() {
        Map results = given()
            .contentType(JSON)
            .body(Map.of(
                "includeEB", true,
                "includeOIDC", true,
                "includePdP", true,
                "includeStepUp", false
            ))
            .when()
            .put("manage/api/client/playground/push")
            .then()
            .statusCode(SC_OK)
            .extract().as(Map.class);

        assertEquals("OK", ((Map) results.get("eb")).get("status"));
        assertEquals("OK", ((Map) results.get("oidc")).get("status"));
        assertEquals("OK", ((Map) results.get("pdp")).get("status"));

        pushWireMockServer.verify(1, postRequestedFor(urlEqualTo("/api/connections")));
        pushWireMockServer.verify(1, postRequestedFor(urlEqualTo("/manage/connections")));
        pdpWireMockServer.verify(1, putRequestedFor(urlEqualTo("/pdp/api/manage/push")));
    }

    @Test
    public void pushReturnsErrorWhenEbFails() {
        pushWireMockServer.stubFor(post(urlEqualTo("/api/connections"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("{\"error\":\"eb unavailable\"}")));

        Map results = push(Map.of("includeEB", true, "includeOIDC", false, "includePdP", false, "includeStepUp", false));

        String message = (String) results.get("message");
        assertTrue(message.contains("Error in push to EngineBlock (http://localhost:9898/api/connections) status 503 SERVICE_UNAVAILABLE"));
        assertTrue(message.contains("{\"error\":\"eb unavailable\"}"));
    }

    @Test
    public void pushReturnsErrorWhenOidcFails() {
        pushWireMockServer.stubFor(post(urlEqualTo("/manage/connections"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("{\"error\":\"oidc unavailable\"}")));

        Map results = push(Map.of("includeEB", false, "includeOIDC", true, "includePdP", false, "includeStepUp", false));

        String message = (String) results.get("message");
        assertTrue(message.contains("Error in push to OIDC (http://localhost:9898/manage/connections) status 503 SERVICE_UNAVAILABLE"));
        assertTrue(message.contains("{\"error\":\"oidc unavailable\"}"));
    }

    @Test
    public void pushReturnsErrorWhenPdpFails() {
        pdpWireMockServer.stubFor(put(urlEqualTo("/pdp/api/manage/push"))
            .atPriority(1)
            .willReturn(aResponse()
                .withStatus(503)
                .withBody("{\"error\":\"pdp unavailable\"}")));

        Map results = push(Map.of("includeEB", false, "includeOIDC", false, "includePdP", true, "includeStepUp", false));

        String message = (String) results.get("message");
        assertTrue(message.contains("Error in push to PDP (http://localhost:8082/pdp/api/manage/push) status 503 SERVICE_UNAVAILABLE"));
        assertTrue(message.contains("{\"error\":\"pdp unavailable\"}"));
    }

    private Map push(Map<String, Boolean> pushOptions) {
        return given()
            .contentType(JSON)
            .body(pushOptions)
            .when()
            .put("manage/api/client/playground/push")
            .then()
            .statusCode(SC_INTERNAL_SERVER_ERROR)
            .extract().as(Map.class);
    }

    @Test
    public void pushPreview() throws Exception {
        Map results = given()
            .when()
            .get("manage/api/client/playground/pushPreview")
            .then()
            .statusCode(SC_OK)
            .extract().as(Map.class);
        Map expected = objectMapper.readValue(readFile("push/push.expected_connections.json"), Map.class);


        assertEquals(expected, results);
        //ensure the Sp with "coin:imported_from_edugain": true is included
        Map connections = (Map) results.get("connections");
        Object importFromEdugain = ((Map) ((Map) ((Map) connections
            .get("11"))
            .get("metadata"))
            .get("coin"))
            .get("imported_from_edugain");
        assertEquals("0", importFromEdugain);

        //ensure the correct ARP is exported
        List<Map<String, Object>> arpGivenNames = (List<Map<String, Object>>) ((Map) ((Map) connections
            .get("11"))
            .get("arp_attributes"))
            .get("urn:mace:dir:attribute-def:givenName");
        Map<String, Object> arpGivenName = arpGivenNames.get(0);
        List<String> keys = arpGivenName.keySet().stream().sorted().collect(Collectors.toList());
        assertEquals(List.of("motivation", "release_as", "use_as_nameid", "value"), keys);
        assertEquals("aliasGivenName", arpGivenName.get("release_as"));
        assertEquals(true, arpGivenName.get("use_as_nameid"));

        Map sramService = (Map) connections
            .get("15");
        String nameSramRP = (String) sramService
            .get("name");
        assertEquals("https://sram.service.api.oidc_rp", nameSramRP);
        Map<String, Object> coinAttributes = (Map<String, Object>) ((Map) sramService.get("metadata")).get("coin");
        assertEquals("1", coinAttributes.get("collab_enabled"));

        String nameSramSP = (String) ((Map) connections
            .get("16"))
            .get("name");
        assertEquals("https://sram.service.api.saml_sp", nameSramSP);

        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) ((Map) connections.get("6")).get("allowed_connections");
        //Verify that that all SRAM services are in the allowedEntities of the IdP, because SRAM RP is in allowed entries
        List<Map<String, String>> sramServices = allowedEntities.stream()
            .filter(entity -> entity.get("name").startsWith("https://sram.service.api."))
            .toList();
        assertEquals(2, sramServices.size());
    }

    @Test
    public void pushPreviewPdP() {
        List<PdpPolicyDefinition> pdpPolicyDefinitions = given()
            .when()
            .get("manage/api/client/playground/pushPreviewPdP")
            .then()
            .statusCode(SC_OK)
            .extract()
            .as(new TypeRef<>() {
            });
        assertEquals(2, pdpPolicyDefinitions.size());
    }

    @Test
    public void pushPreviewSFO() {
        Map<String, Object> sfoEntities = given()
            .when()
            .get("manage/api/client/playground/pushPreviewSFO")
            .then()
            .statusCode(SC_OK)
            .extract()
            .as(new TypeRef<>() {
            });
        assertEquals(3, sfoEntities.size());
        assertEquals(6, ((List) sfoEntities.get("sraa")).size());
        assertEquals(9, ((Map) sfoEntities.get("email_templates")).size());
        Map gateway = (Map) sfoEntities.get("gateway");
        assertEquals(2, gateway.size());
        assertEquals(1+2, ((List) gateway.get("service_providers")).size());
        assertEquals(0, ((List) gateway.get("identity_providers")).size());
    }

    @Test
    public void pushPreviewStepup() {
        Map<String, List<String>> uniqueInstitutions = given()
            .when()
            .get("manage/api/client/playground/pushPreviewStepup")
            .then()
            .statusCode(SC_OK)
            .extract()
            .as(new TypeRef<>() {
            });
        assertEquals(1, uniqueInstitutions.size());
        assertEquals(List.of("inst1"), uniqueInstitutions.get("institutions"));
    }

    @Test
    public void pushPreviewInstitution() {
        Map<String, Map<String, Object>> institutions = given()
            .when()
            .get("manage/api/client/playground/pushPreviewInstitution")
            .then()
            .statusCode(SC_OK)
            .extract()
            .as(new TypeRef<>() {
            });
        assertEquals(1, institutions.size());
        assertEquals(11, institutions.get("inst1").size());
    }
}
