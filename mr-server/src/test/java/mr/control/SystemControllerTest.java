package mr.control;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import mr.AbstractIntegrationTest;
import org.junit.Rule;
import org.junit.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.isEmptyOrNullString;
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
            .body(isEmptyOrNullString());

    }

    @Test
    public void pushPreview() throws Exception {
        given()
            .when()
            .get("mr/api/client/playground/pushPreview")
            .then()
            .statusCode(SC_OK)
            .body("connections.1.arp_attributes.urn:mace:dir:attribute-def:displayName[0].value",
                equalTo("*"));

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