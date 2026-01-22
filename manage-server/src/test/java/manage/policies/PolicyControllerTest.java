package manage.policies;

import io.restassured.common.mapper.TypeRef;
import manage.AbstractIntegrationTest;
import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static manage.api.APIAuthenticationManager.*;
import static org.junit.jupiter.api.Assertions.*;

public class PolicyControllerTest extends AbstractIntegrationTest {

    @Test
    public void policies() {
        List<PdpPolicyDefinition> policies = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/internal/protected/policies")
                .as(new TypeRef<>() {
                });
        assertEquals(2, metaDataRepository.getMongoTemplate().count(new Query(), EntityType.PDP.getType()));
        //One is filtered out because the user from this.headers has no access to the IdP
        assertEquals(1, policies.size());
    }

    @Test
    public void policyById() {
        PdpPolicyDefinition policy = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/internal/protected/policies/13")
                .as(PdpPolicyDefinition.class);
        assertEquals("http://mock-idp", policy.getIdentityProviderIds().get(0));
        assertTrue(policy.isActionsAllowed());
    }

    @Test
    public void stepPolicyById() {
        PdpPolicyDefinition policy = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers("https://idp.test2.surfconext.nl"))
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/internal/protected/policies/14")
                .as(PdpPolicyDefinition.class);
        assertEquals("https://idp.test2.surfconext.nl", policy.getIdentityProviderIds().get(0));
        assertFalse(policy.isActionsAllowed());
        //The ipInfo is appended to stepUp policies
        List<IPInfo> ipInfos = policy.getLoas().stream()
                .flatMap(loa -> loa.getCidrNotations().stream().map(cidrNotation -> cidrNotation.getIpInfo()))
                .toList();
        assertEquals(2, ipInfos.size());

    }

    @Test
    public void policyByIdNotAllowed() {
        given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/internal/protected/policies/14")
                .then()
                        .statusCode(403);
    }

    @Test
    public void createRegPolicy() {
        Map<String, Object> data = readValueFromFile("/policies/dashboard_reg_post.json");
        PdpPolicyDefinition policy = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .body(data)
                .header("Content-type", "application/json")
                .post("manage/api/internal/protected/policies")
                .as(PdpPolicyDefinition.class);
        MetaData retrievedMetaData = given()
                .when()
                .get("manage/api/client/metadata/policy/" + policy.getId())
                .as(MetaData.class);
        assertEquals("urn:john", retrievedMetaData.getData().get("userDisplayName"));
        assertEquals(1, ((List<?>) retrievedMetaData.getData().get("serviceProviderIds")).size());
        assertEquals(1, ((List<?>) retrievedMetaData.getData().get("identityProviderIds")).size());
        assertEquals(1, ((List<?>) retrievedMetaData.getData().get("attributes")).size());
        assertEquals(0, ((List<?>) retrievedMetaData.getData().get("loas")).size());
    }

    @Test
    public void createStepPolicy() {
        Map<String, Object> data = readValueFromFile("/policies/dashboard_step_post.json");
        PdpPolicyDefinition policy = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .body(data)
                .header("Content-type", "application/json")
                .post("manage/api/internal/protected/policies")
                .as(PdpPolicyDefinition.class);
        MetaData retrievedMetaData = given()
                .when()
                .get("manage/api/client/metadata/policy/" + policy.getId())
                .as(MetaData.class);
        assertEquals("urn:john", retrievedMetaData.getData().get("userDisplayName"));
        assertEquals(1, ((List<?>) retrievedMetaData.getData().get("serviceProviderIds")).size());
        assertEquals(1, ((List<?>) retrievedMetaData.getData().get("identityProviderIds")).size());
        assertEquals(0, ((List<?>) retrievedMetaData.getData().get("attributes")).size());
        assertEquals(1, ((List<?>) retrievedMetaData.getData().get("loas")).size());
    }

    @Test
    public void update() {
        PdpPolicyDefinition policy = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/internal/protected/policies/13")
                .as(PdpPolicyDefinition.class);
        policy.setAuthenticatingAuthorityName("Not allowed to change");
        policy.setDescription("Changed");

        PdpPolicyDefinition updatedPolicy = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .body(policy)
                .when()
                .header("Content-type", "application/json")
                .put("manage/api/internal/protected/policies")
                .as(PdpPolicyDefinition.class);
        assertEquals(policy.getDescription(), updatedPolicy.getDescription());
        assertEquals("http://mock-idp", updatedPolicy.getAuthenticatingAuthorityName());

        List<PdpPolicyDefinition> revisions = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/internal/protected/revisions/13")
                .as(new TypeRef<>() {
                });
        assertEquals(2, revisions.size());
    }

    @Test
    public void delete() {
        assertEquals(2, metaDataRepository.getMongoTemplate().count(new Query(), EntityType.PDP.getType()));
        given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .header("Content-type", "application/json")
                .delete("manage/api/internal/protected/policies/13")
                .then()
                .statusCode(200);
        assertEquals(1, metaDataRepository.getMongoTemplate().count(new Query(), EntityType.PDP.getType()));
    }

    @Test
    public void deleteNotAllowed() {
        assertEquals(2, metaDataRepository.getMongoTemplate().count(new Query(), EntityType.PDP.getType()));
        given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .headers(this.headers())
                .when()
                .header("Content-type", "application/json")
                .delete("manage/api/internal/protected/policies/14")
                .then()
                .statusCode(403);
        assertEquals(2, metaDataRepository.getMongoTemplate().count(new Query(), EntityType.PDP.getType()));
    }

    @Test
    public void getAllowedAttributesDashBoard() {
        List<Map<String, String>> allowedAttributes = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/internal/protected/attributes")
                .as(new TypeRef<>() {
                });
        assertEquals(9, allowedAttributes.size());
    }

    @Test
    public void getAllowedAttributesAccess() {
        List<Map<String, String>> allowedAttributes = given()
            .auth()
            .preemptive()
            .basic("dashboard", "secret")
            .when()
            .header("Content-type", "application/json")
            .get("manage/api/internal/protected/allowed-attributes")
            .as(new TypeRef<>() {
            });
        assertEquals(9, allowedAttributes.size());
        assertTrue(allowedAttributes.stream().allMatch(m -> m.containsKey("validationRegex")
            && m.containsKey("allowedInDenyRule")));
    }

    @Test
    public void getAllowedSamlAttributes() {
        List<Map<String, String>> allowedSamlAttributes = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/internal/protected/saml-attributes")
                .as(new TypeRef<>() {
                });
        assertEquals(11, allowedSamlAttributes.size());
    }

    private Map<String, String> headers() {
        return this.headers("http://mock-idp");
    }

    private Map<String, String> headers(String entityId) {
        return Map.of(
                X_DISPLAY_NAME, "John Doe",
                X_UNSPECIFIED_NAME_ID, "urn:john",
                X_IDP_ENTITY_ID, entityId
        );
    }
}
