package manage.control;

import manage.AbstractIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class DatabaseControllerTest extends AbstractIntegrationTest {
    @Value("${push.eb.user}")
    private String pushUser;

    @Value("${push.eb.password}")
    private String pushPassword;

    @Test
    public void pushPreview() throws Exception {
        Map connections = given()
                .when()
                .get("manage/api/client/playground/pushPreview")
                .then()
                .statusCode(SC_OK)
                .extract().as(Map.class);
        Map expected = objectMapper.readValue(readFile("push/push.expected_connections.json"), Map.class);

        assertEquals(expected, connections);
    }

}
