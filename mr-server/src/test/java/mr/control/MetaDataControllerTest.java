package mr.control;

import mr.AbstractIntegrationTest;
import mr.model.MetaData;
import mr.model.Revision;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MetaDataControllerTest extends AbstractIntegrationTest {

    @Test
    public void get() throws Exception {
        given()
            .when()
            .get("mr/api/client/metadata/service_provider/1")
            .then()
            .statusCode(SC_OK)
            .body("id", equalTo("1"))
            .body("revision.number", equalTo(0))
            .body("data.entityid", equalTo("Duis ad do"));
    }

    @Test
    public void post() throws Exception {
        String json = fileContent("/json/valid_service_provider.json");
        Map data = objectMapper.readValue(json, Map.class);
        MetaData metaData = new MetaData("service_provider", data);
        String id = given()
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("mr/api/client/metadata")
            .then()
            .statusCode(SC_OK)
            .extract().path("id");

        MetaData savedMetaData = metaDataRepository.findById(id, "service_provider");
        Revision revision = savedMetaData.getRevision();
        assertEquals("saml2_user.com", revision.getUpdatedBy());
        assertEquals(0, revision.getNumber());
        assertNotNull(revision.getCreated());
    }

    @Test
    public void put() throws Exception {
        MetaData metaData = metaDataRepository.findById("1", "service_provider");
        Map.class.cast(metaData.getData()).put("entityid", "changed");
        given()
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .put("mr/api/client/metadata")
            .then()
            .statusCode(SC_OK)
            .body("revision.number", equalTo(1))
            .body("revision.created", notNullValue())
            .body("revision.updatedBy", equalTo("saml2_user.com"))
            .body("data.entityid", equalTo("changed"));

        List<MetaData> revisions = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "service_provider_revision");
        assertEquals(1, revisions.size());

        Revision revision = revisions.get(0).getRevision();
        assertEquals("saml2_user.com", revision.getUpdatedBy());
        assertEquals(0, revision.getNumber());
        assertEquals("1", revision.getParentId());
    }
}