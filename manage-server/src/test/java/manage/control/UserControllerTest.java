package manage.control;

import manage.AbstractIntegrationTest;
import manage.conf.Product;
import manage.conf.Push;
import manage.shibboleth.FederatedUser;
import org.junit.Test;

import java.util.Collections;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.junit.Assert.assertFalse;

public class UserControllerTest extends AbstractIntegrationTest {

    @Test
    public void checkPasswordNotPresentInAccount() {
        Product mockProduct = new Product("OpenConext", "Manage", "https://feed.nl", true);
        Push mockPush = new Push("https://engine-url.nl", "Engineblock", "https://oidc-url.nl", "OIDC", false);
        FederatedUser mockUser = new FederatedUser("uid", "displayName", "org",
                Collections.emptyList(), Collections.emptyList(), mockProduct, mockPush);

        String result = given()
                .when()
                .body(mockUser)
                .header("Content-type", "application/json")
                .get("manage/api/client/users/me")
                .then()
                .statusCode(SC_OK)
                .extract().body().asString();

        assertFalse(result.contains("password"));
    }

}
