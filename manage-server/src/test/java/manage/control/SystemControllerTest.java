package manage.control;

import manage.AbstractIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class SystemControllerTest extends AbstractIntegrationTest {

    @Test
    public void validate() throws Exception {
        String body = given()
                .when()
                .get("manage/api/client/playground/validate")
                .then()
                .statusCode(SC_OK)
                .extract().asString();
        assertEquals("{}", body);
    }

    @Test
    public void orphans() {
        List orphans = given()
                .when()
                .get("manage/api/client/playground/orphans")
                .then()
                .statusCode(SC_OK)
                .extract().as(List.class);
        assertEquals(5, orphans.size());

        given()
                .when()
                .delete("manage/api/client/playground/deleteOrphans")
                .then()
                .statusCode(SC_OK);

        given()
                .when()
                .get("manage/api/client/playground/orphans")
                .then()
                .statusCode(SC_OK)
                .body("size()", is(0));
    }
}