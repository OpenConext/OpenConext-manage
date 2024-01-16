package manage;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

class ApplicationIntegrationTest {

    @Test
    void main() {
        Application.main(new String[]{"--server.port=8098"});
        RestAssured.port = 8098;

        given()
                .when()
                .accept(ContentType.JSON)
                .contentType(ContentType.JSON)
                .get("/internal/health")
                .then()
                .body("status", equalTo("UP"));
    }
}