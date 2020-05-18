package manage.control;

import io.restassured.RestAssured;
import manage.AbstractIntegrationTest;
import manage.model.Scope;
import org.junit.Before;
import org.junit.Test;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_OK;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ScopeControllerTest extends AbstractIntegrationTest {

    @Before
    public void before() {
        RestAssured.port = port;
        MongoTemplate mongoTemplate = mongoTemplate();
        List<Scope> scopes = Arrays.asList(
                new Scope("1",0L,"groups", descriptions()),
                new Scope("2", 0L, "persons", descriptions())
        );
        mongoTemplate.bulkOps(BulkOperations.BulkMode.UNORDERED, Scope.class)
                .remove(Query.query(Criteria.where("_id").exists(true)))
                .insert(scopes)
                .execute();
        await().until(() -> mongoTemplate.count(new Query(), Scope.class) == scopes.size());
    }

    @Test
    public void save() {
        String id = given()
                .when()
                .body(new Scope("emails", descriptions()))
                .header("Content-type", "application/json")
                .post("manage/api/client/scopes")
                .then()
                .statusCode(SC_OK)
                .extract().path("id");
        assertNotNull(id);
    }

    @Test
    public void saveDuplicateKey() {
        given()
                .when()
                .body(new Scope("groups", descriptions()))
                .header("Content-type", "application/json")
                .post("manage/api/client/scopes")
                .then()
                .statusCode(SC_INTERNAL_SERVER_ERROR)
                .body("exception", is("org.springframework.dao.DuplicateKeyException"));
    }

    @Test
    public void all() {
        List<Scope> scopes = given()
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/client/scopes")
                .then()
                .extract().body().jsonPath().getList(".", Scope.class);
        assertEquals(2, scopes.size());
    }

    @Test
    public void update() {
        Scope scope = given()
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/client/scopes/1")
                .as(Scope.class);

        ReflectionTestUtils.setField(scope, "name", "changed");

        given()
                .when()
                .body(scope)
                .header("Content-type", "application/json")
                .put("manage/api/client/scopes")
                .then()
                .statusCode(SC_OK)
                .extract().path("id");

        scope = given()
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/client/scopes/1")
                .as(Scope.class);
        assertEquals("changed", scope.getName());
        assertEquals(new Long(1), scope.getVersion());
    }

    @Test
    public void languages() {
        List<String> languages = given()
                .when()
                .header("Content-type", "application/json")
                .get("manage/api/client/scopes_languages")
                .then()
                .extract().body().jsonPath().getList(".", String.class);
        assertEquals(3, languages.size());
    }
    private Map<String, String> descriptions() {
        Map<String, String> descriptions = new HashMap<>();
        descriptions.put("en", "English description");
        descriptions.put("nl", "Nederlandse omschrijving");
        return descriptions;
    }
}
