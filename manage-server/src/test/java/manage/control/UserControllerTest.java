package manage.control;

import io.restassured.common.mapper.TypeRef;
import manage.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
        assertEquals("saml2_user.com", federatedUser.get("name"));
    }
}