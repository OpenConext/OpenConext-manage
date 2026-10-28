package manage.control;

import io.restassured.common.mapper.TypeRef;
import manage.AbstractIntegrationTest;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.policies.PdpPolicyDefinition;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    private static final String SRAM_RP = "https@//oidc.rp";
    private static final String SRAM_OIDC_RP = "https://sram.service.api.oidc_rp";
    private static final String SRAM_SAML_SP = "https://sram.service.api.saml_sp";
    private static final String LOA2 = "http://test.surfconext.nl/assurance/loa2";
    private static final String LOA3 = "http://test.surfconext.nl/assurance/loa3";
    private static final String MFA = "https://refeds.org/profile/mfa";

    @Test
    public void pushPreviewSramTemplateLoaCopied() {
        updateIdentityProvider("https://idp.test2.surfconext.nl", data -> {
            //The template entry is matched case-insensitively and an explicit entry for a SRAM service must be replaced
            data.put("stepupEntities", new ArrayList<>(List.of(
                Map.of("name", "http://other-sp", "level", LOA2),
                Map.of("name", SRAM_RP.toUpperCase(), "level", LOA3),
                Map.of("name", SRAM_SAML_SP, "level", LOA2))));
            data.put("mfaEntities", new ArrayList<>(List.of(
                Map.of("name", SRAM_RP, "level", MFA),
                Map.of("name", SRAM_OIDC_RP, "level", "http://schemas.microsoft.com/claims/multipleauthn"))));
        });

        Map idp = pushPreviewConnection("6");

        List<Map<String, String>> stepup = (List<Map<String, String>>) idp.get("stepup_connections");
        assertEquals(4, stepup.size());
        assertEquals(LOA2, levelOf(stepup, "http://other-sp"));
        assertEquals(LOA3, levelOf(stepup, SRAM_RP.toUpperCase()));
        assertEquals(LOA3, levelOf(stepup, SRAM_OIDC_RP));
        assertEquals(LOA3, levelOf(stepup, SRAM_SAML_SP));
        assertEquals(1, countOf(stepup, SRAM_SAML_SP));

        List<Map<String, String>> mfa = (List<Map<String, String>>) idp.get("mfa_entities");
        assertEquals(3, mfa.size());
        assertEquals(MFA, levelOf(mfa, SRAM_RP));
        assertEquals(MFA, levelOf(mfa, SRAM_OIDC_RP));
        assertEquals(MFA, levelOf(mfa, SRAM_SAML_SP));
        assertEquals(1, countOf(mfa, SRAM_OIDC_RP));
    }

    @Test
    public void pushPreviewSramWithoutTemplateEntryLeavesLoaUntouched() {
        //The seed IdP has stepup and mfa entries, but none for the SRAM RP
        Map idp = pushPreviewConnection("6");

        assertEquals(2, ((List<?>) idp.get("allowed_connections")).stream()
            .filter(entity -> ((Map) entity).get("name").toString().startsWith("https://sram.service.api.")).count());
        List<Map<String, String>> stepup = (List<Map<String, String>>) idp.get("stepup_connections");
        assertEquals(3, stepup.size());
        assertEquals(0, countOf(stepup, SRAM_OIDC_RP) + countOf(stepup, SRAM_SAML_SP));
        List<Map<String, String>> mfa = (List<Map<String, String>>) idp.get("mfa_entities");
        assertEquals(1, mfa.size());
        assertEquals(0, countOf(mfa, SRAM_OIDC_RP) + countOf(mfa, SRAM_SAML_SP));
    }

    @Test
    public void pushPreviewSramWithoutLoaConfiguration() {
        updateIdentityProvider("https://idp.test2.surfconext.nl", data -> {
            data.remove("stepupEntities");
            data.remove("mfaEntities");
        });

        Map idp = pushPreviewConnection("6");

        assertEquals(6, ((List<?>) idp.get("allowed_connections")).size());
        assertEquals(List.of(), idp.get("stepup_connections"));
        assertEquals(List.of(), idp.get("mfa_entities"));
    }

    @Test
    public void pushPreviewSramNotAllowedByIdentityProvider() {
        updateIdentityProvider("https://idp.test2.surfconext.nl", data -> {
            ((List<Map<String, String>>) data.get("allowedEntities")).removeIf(entity -> SRAM_RP.equals(entity.get("name")));
            data.put("stepupEntities", new ArrayList<>(List.of(Map.of("name", SRAM_RP, "level", LOA3))));
            data.put("mfaEntities", new ArrayList<>(List.of(Map.of("name", SRAM_RP, "level", MFA))));
        });

        Map idp = pushPreviewConnection("6");

        List<Map<String, String>> allowed = (List<Map<String, String>>) idp.get("allowed_connections");
        assertEquals(3, allowed.size());
        assertFalse(allowed.stream().anyMatch(entity -> entity.get("name").startsWith("https://sram.service.api.")));
        assertEquals(1, ((List<?>) idp.get("stepup_connections")).size());
        assertEquals(1, ((List<?>) idp.get("mfa_entities")).size());
    }

    @Test
    public void pushPreviewSramAllowedAllIdentityProvider() {
        updateIdentityProvider("http://mock-idp", data -> {
            data.put("stepupEntities", new ArrayList<>(List.of(Map.of("name", SRAM_RP, "level", LOA3))));
            data.put("mfaEntities", new ArrayList<>(List.of(Map.of("name", SRAM_RP, "level", MFA))));
        });

        Map idp = pushPreviewConnection("7");

        assertEquals(true, idp.get("allow_all_entities"));
        assertEquals(List.of(), idp.get("allowed_connections"));
        assertEquals(1, ((List<?>) idp.get("stepup_connections")).size());
        assertEquals(1, ((List<?>) idp.get("mfa_entities")).size());
    }

    private Map pushPreviewConnection(String id) {
        Map results = given()
            .when()
            .get("manage/api/client/playground/pushPreview")
            .then()
            .statusCode(SC_OK)
            .extract().as(Map.class);
        return (Map) ((Map) results.get("connections")).get(id);
    }

    private void updateIdentityProvider(String entityId, java.util.function.Consumer<Map<String, Object>> updater) {
        String type = EntityType.IDP.getType();
        MetaData metaData = mongoTemplate().findOne(Query.query(Criteria.where("data.entityid").is(entityId)), MetaData.class, type);
        updater.accept(metaData.getData());
        mongoTemplate().save(metaData, type);
    }

    private String levelOf(List<Map<String, String>> entries, String name) {
        return entries.stream().filter(entry -> name.equals(entry.get("name"))).findFirst().orElseThrow().get("level");
    }

    private long countOf(List<Map<String, String>> entries, String name) {
        return entries.stream().filter(entry -> name.equalsIgnoreCase(entry.get("name"))).count();
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
