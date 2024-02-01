package manage.policies;

import manage.AbstractIntegrationTest;
import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;

public class PoliciesControllerTest extends AbstractIntegrationTest {

    @Test
    public void policies() {
    }

    @Test
    public void testPolicies() {
    }

    @Test
    public void create() {
        Map<String, Object> data = readValueFromFile("/json/valid_policy_step.json");
        MetaData metaData = new MetaData(EntityType.PDP.getType(), data);
        String path = given()
                .auth()
                .preemptive()
                .basic("dashboard", "secret")
                .when()
                .body(metaData)
                .header("Content-type", "application/json")
                .post("manage/api/internal/protected/policies")
                .then()
                .statusCode(SC_OK)
                .extract()
                .path("id");
        System.out.println(path);
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