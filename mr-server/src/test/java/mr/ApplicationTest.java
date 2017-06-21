package mr;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, value = {"spring.profiles.active=dev"})
public class ApplicationTest extends AbstractIntegrationTest {

    @Test
    public void health() throws Exception {
        given()
            .when()
            .get("mr/api/health")
            .then()
            .statusCode(SC_OK)
            .body("status", equalTo("UP"));
    }

    @Test
    public void testMain() {
        Application application = new Application();
        application.main(new String[]{});
    }

}