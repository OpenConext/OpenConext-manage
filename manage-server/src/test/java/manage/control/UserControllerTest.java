package manage.control;

import io.restassured.common.mapper.TypeRef;
import manage.AbstractIntegrationTest;
import org.junit.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertFalse;

public class UserControllerTest extends AbstractIntegrationTest {

    @Test
    public void me() {
        Map<String, Object> federatedUser = given()
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/client/users/me")
                .as(new TypeRef<>() {
                });
        assertFalse(federatedUser.containsKey("password"));

    }
}