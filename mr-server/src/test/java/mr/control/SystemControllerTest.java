package mr.control;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import mr.AbstractIntegrationTest;
import org.junit.Rule;
import org.junit.Test;

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

public class SystemControllerTest extends AbstractIntegrationTest {

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(9898);

    @Test
    public void push() throws Exception {
        stubFor(post(urlPathEqualTo("/api/connections"))
            .willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")));

        given()
            .when()
            .get("mr/api/client/playground/push")
            .then()
            .statusCode(SC_OK)
            .body("status", equalTo("BAD_REQUEST"));

    }

    @Test
    public void pushPreview() throws Exception {
        Map connections = given()
            .when()
            .get("mr/api/client/playground/pushPreview")
            .then()
            .statusCode(SC_OK)
            .extract().as(Map.class);

        Map<String, Object> innerConnections = Map.class.cast(connections.get("connections"));

        List<String> idsToRemove = Arrays.asList("2", "3", "4", "5");
        innerConnections.entrySet().removeIf(entry -> idsToRemove.contains(entry.getKey()));
        System.out.println(objectMapper.writeValueAsString(connections));
        Map expected = objectMapper.readValue(readFile("push/push.expected.json"), Map.class);
        assertEquals(expected, connections);
    }

    @Test
    public void validate() throws Exception {
        String body = given()
            .when()
            .get("mr/api/client/playground/validate")
            .then()
            .statusCode(SC_OK)
            .extract().asString();
        assertEquals("{}", body);
    }

}