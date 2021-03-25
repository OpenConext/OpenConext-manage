package manage;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations= "classpath:test.properties")
public class ApplicationTest extends AbstractIntegrationTest {

    @Test
    public void health() throws Exception {
        given()
                .when()
                .get("manage/api/actuator/health")
                .then()
                .statusCode(SC_OK)
                .body("status", equalTo("UP"));
    }

}