package mr.control;

import mr.AbstractIntegrationTest;
import org.junit.Test;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;

public class MetaDataControllerTest extends AbstractIntegrationTest {

    @Test
    public void get() throws Exception {
        given()
            .when()
            .get("mr/api/client/metadata/service_provider/1")
            .then()
            .statusCode(SC_OK)
            .body("id", equalTo("1"))
            .body("data.entityid", equalTo("https://engine.test2.surfconext.nl/authentication/sp/metadata"));

    }

}