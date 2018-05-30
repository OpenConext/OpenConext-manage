package manage.control;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.restassured.specification.RequestSpecification;
import manage.AbstractIntegrationTest;
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
import static org.junit.Assert.assertEquals;

@SuppressWarnings("unchecked")
public class SystemControllerTest extends AbstractIntegrationTest {

    @Value("${push.user}")
    private String pushUser;

    @Value("${push.password}")
    private String pushPassword;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9898);

    @Test
    public void push() throws Exception {
        doPush(true);
    }

    @Test
    public void pushInternal() throws Exception {
        doPush(false);
    }

    private void doPush(boolean client) {
        stubFor(post(urlPathEqualTo("/api/connections")).withBasicAuth(pushUser, pushPassword)
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")));

        RequestSpecification given = given();
        if (!client) {
            given
                .auth()
                .preemptive()
                .basic("sp-portal", "secret");
        }
        given
            .when()
            .get("manage/api/" + (client ? "client/playground" : "internal") + "/push")
            .then()
            .statusCode(SC_OK)
            .body("status", equalTo("OK"));
    }

    @Test
    public void pushPreview() throws Exception {
        Map connections = given()
            .when()
            .get("manage/api/client/playground/pushPreview")
            .then()
            .statusCode(SC_OK)
            .extract().as(Map.class);

        Map<String, Object> innerConnections = Map.class.cast(connections.get("connections"));

        List<String> idsToRemove = Arrays.asList("2", "3", "4", "5");
        innerConnections.entrySet().removeIf(entry -> idsToRemove.contains(entry.getKey()));

        Map expected = objectMapper.readValue(readFile("push/push.expected.json"), Map.class);
        System.out.println(objectMapper.writeValueAsString(expected));
//        assertEquals(expected, connections);
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

}