package manage.control;

import manage.AbstractIntegrationTest;
import org.junit.Test;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertEquals;

public class StatsControllerTest extends AbstractIntegrationTest {

    @Test
    public void connections() throws Exception {
        List connections = given()
            .auth()
            .preemptive()
            .basic("sysadmin", "secret")
            .when()
            .get("manage/api/internal/stats/connections")
            .then()
            .statusCode(SC_OK)
            .extract().response().as(List.class);
        assertEquals(5, connections.size());

    }
}
