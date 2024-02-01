package manage.policies;

import manage.AbstractIntegrationTest;
import manage.model.MetaData;
import org.junit.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static manage.api.APIAuthenticationManager.*;

public class PoliciesControllerTest extends AbstractIntegrationTest {

    @Test
    public void policies() {
    }

    @Test
    public void testPolicies() {
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
        System.out.println(retrievedMetaData);
    }

    private Map<String, String> headers() {
        return Map.of(
                X_DISPLAY_NAME, "John Doe",
                X_UNSPECIFIED_NAME_ID, "urn:john",
                X_IDP_ENTITY_ID, "http://mock-idp"
        );
    }

    @Test
    public void update() {
    }

    @Test
    public void delete() {
    }

    @Test
    public void revisions() {
    }

    @Test
    public void getAllowedAttributes() {
    }

    @Test
    public void getAllowedSamlAttributes() {
    }
}