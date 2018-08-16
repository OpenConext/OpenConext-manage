package manage.control;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.specification.RequestSpecification;
import manage.AbstractIntegrationTest;
import manage.model.OrphanMetaData;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class SystemControllerTest extends AbstractIntegrationTest {

    @Value("${push.user}")
    private String pushUser;

    @Value("${push.password}")
    private String pushPassword;

    @Test
    public void pushPreview() throws Exception {
        Map connections = given()
            .when()
            .get("manage/api/client/playground/pushPreview")
            .then()
            .statusCode(SC_OK)
            .extract().as(Map.class);
        Map expected = objectMapper.readValue(readFile("push/push.expected.json"), Map.class);

        assertEquals(expected, connections);
    }

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
    public void orphans() throws Exception {
        List orphans = given()
            .when()
            .get("manage/api/client/playground/orphans")
            .then()
            .statusCode(SC_OK)
            .extract().as(List.class);
        List expected = objectMapper.readValue(readFile("json/expected_orphans.json"), List.class);

        assertEquals(expected, orphans);

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