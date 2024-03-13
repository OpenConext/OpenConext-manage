package manage.control;

import manage.AbstractIntegrationTest;
import org.junit.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class DatabaseControllerTest extends AbstractIntegrationTest {

    @Test
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
        assertEquals(importFromEdugain, "1");
    }

}
