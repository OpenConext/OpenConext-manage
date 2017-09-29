package manage.control;

import io.restassured.specification.RequestSpecification;
import manage.AbstractIntegrationTest;
import manage.migration.EntityType;
import manage.model.MetaData;
import manage.model.MetaDataUpdate;
import manage.model.Revision;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static manage.control.MetaDataController.REQUESTED_ATTRIBUTES;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class MetaDataControllerTest extends AbstractIntegrationTest {

    @Test
    public void get() throws Exception {
        given()
            .when()
            .get("manage/api/client/metadata/saml20_sp/1")
            .then()
            .statusCode(SC_OK)
            .body("id", equalTo("1"))
            .body("revision.number", equalTo(0))
            .body("data.entityid", equalTo("Duis ad do"));
    }

    @Test
    public void getNotFound() throws Exception {
        given()
            .when()
            .get("manage/api/client/metadata/saml20_sp/x")
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void templateSp() throws Exception {
        given()
            .when()
            .get("manage/api/client/template/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("type", equalTo("saml20_sp"))
            .body("data.arp.enabled", equalTo(false))
            .body("data.arp.attributes", notNullValue())
            .body("data.metaDataFields", notNullValue())
            .body("data.entityid", isEmptyOrNullString())
            .body("data.state", equalTo("testaccepted"))
            .body("data.allowedEntities.size()", is(0))
            .body("data.disableConsent", isEmptyOrNullString());
    }

    @Test
    public void templateIdp() throws Exception {
        given()
            .when()
            .get("manage/api/client/template/saml20_idp")
            .then()
            .statusCode(SC_OK)
            .body("type", equalTo("saml20_idp"))
            .body("data.allowedall", equalTo(true))
            .body("data.metaDataFields", notNullValue())
            .body("data.entityid", isEmptyOrNullString())
            .body("data.state", equalTo("testaccepted"))
            .body("data.allowedEntities.size()", is(0))
            .body("data.disableConsent.size()", is(0));
    }

    @Test
    public void remove() throws Exception {
        MetaData metaData = metaDataRepository.findById("1", "saml20_sp");
        assertNotNull(metaData);

        given()
            .when()
            .delete("manage/api/client/metadata/saml20_sp/1")
            .then()
            .statusCode(SC_OK);

        metaData = metaDataRepository.findById("1", "saml20_sp");
        assertNull(metaData);
    }


    @Test
    public void configuration() throws Exception {
        given()
            .when()
            .get("manage/api/client/metadata/configuration")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(2))
            .body("title", hasItems("saml20_sp", "saml20_idp"));
    }

    @Test
    public void post() throws Exception {
        doPost(true, "saml2_user.com");
    }

    @Test
    public void postInternal() throws Exception {
        doPost(false, "sp-portal");
    }

    private void doPost(boolean client, String expectedUpdatedBy) throws java.io.IOException {
        String id = createServiceProviderMetaData(client);

        MetaData savedMetaData = metaDataRepository.findById(id, EntityType.SP.getType());
        Revision revision = savedMetaData.getRevision();
        assertEquals(expectedUpdatedBy, revision.getUpdatedBy());
        assertEquals(0, revision.getNumber());
        assertNotNull(revision.getCreated());
    }


    private String createServiceProviderMetaData(boolean client) throws java.io.IOException {
        String json = readFile("/json/valid_service_provider.json");
        Map data = objectMapper.readValue(json, Map.class);
        MetaData metaData = new MetaData(EntityType.SP.getType(), data);
        RequestSpecification given = given();
        System.out.println(objectMapper.writeValueAsString(metaData));
        if (!client) {
            given
                .auth()
                .preemptive()
                .basic("sp-portal", "secret");
        }
        return given
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/" + (client ? "client" : "internal") + "/metadata")
            .then()
            .statusCode(SC_OK)
            .extract().path("id");
    }

    @Test
    public void update() throws Exception {
        String id = createServiceProviderMetaData(true);
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.description:en", "New description");
        pathUpdates.put("allowedall", false);
        pathUpdates.put("allowedEntities", Arrays.asList(Collections.singletonMap("name", "https://allow-me")));
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate(id, EntityType.SP.getType(), pathUpdates);

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .body(metaDataUpdate)
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_OK)
            .body("id", equalTo(id))
            .body("revision.number", equalTo(1))
            .body("revision.updatedBy", equalTo("sp-portal"))
            .body("data.allowedall", equalTo(false))
            .body("data.metaDataFields.'description:en'", equalTo("New description"))
            .body("data.allowedEntities[0].name", equalTo("https://allow-me"));
    }

    @Test
    public void updateWithValidationErrors() throws Exception {
        String id = createServiceProviderMetaData(true);
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.NameIDFormats:0", "bogus");
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate(id, EntityType.SP.getType(), pathUpdates);

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .body(metaDataUpdate)
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("validations", notNullValue());
    }

    @Test
    public void updateWithoutCorrectScope() throws Exception {
        given()
            .auth()
            .preemptive()
            .basic("pdp", "secret")
            .body(new MetaDataUpdate("id", EntityType.SP.getType(), new HashMap<>()))
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void updateWithoutCorrectUser() throws Exception {
        given()
            .auth()
            .preemptive()
            .basic("bogus", "nope")
            .body(new HashMap<>())
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void putClient() throws Exception {
        doPut(true, "saml2_user.com");
    }

    @Test
    public void putInternal() throws Exception {
        doPut(false, "sp-portal");
    }

    @SuppressWarnings("unchecked")
    private void doPut(boolean client, String expectedUpdatedBy) {
        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        Map.class.cast(metaData.getData()).put("entityid", "changed");
        RequestSpecification given = given();
        if (!client) {
            given
                .auth()
                .preemptive()
                .basic("sp-portal", "secret");
        }
        given
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .put("/manage/api/" + (client ? "client" : "internal") + "/metadata")
            .then()
            .statusCode(SC_OK)
            .body("revision.number", equalTo(1))
            .body("revision.created", notNullValue())
            .body("revision.updatedBy", equalTo(expectedUpdatedBy))
            .body("data.entityid", equalTo("changed"));

        List<MetaData> revisions = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_sp_revision");
        assertEquals(1, revisions.size());

        Revision revision = revisions.get(0).getRevision();
        assertEquals("saml2_user.com", revision.getUpdatedBy());
        assertEquals(0, revision.getNumber());
        assertEquals("1", revision.getParentId());

        given()
            .when()
            .get("manage/api/client/revisions/saml20_sp/1")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(1))
            .body("[0].id", notNullValue())
            .body("[0].revision.number", equalTo(0))
            .body("[0].data.entityid", equalTo("Duis ad do"));
    }

    @Test
    public void autoComplete() throws Exception {
        given()
            .when()
            .queryParam("query", "mock")
            .get("manage/api/client/autocomplete/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(2))
            .body("'_id'", hasItems("3", "5"))
            .body("data.entityid", hasItems(
                "http://mock-sp",
                "https://serviceregistry.test2.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp-2"));
    }

    @Test
    public void search() throws Exception {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("metaDataFields.coin:do_not_add_attribute_aliases", "1");
        searchOptions.put("metaDataFields.contacts:3:contactType", "technical");
        searchOptions.put("metaDataFields.NameIDFormat", "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified");

        searchOptions.put(REQUESTED_ATTRIBUTES, Arrays.asList(
            "allowedall", "metaDataFields.AssertionConsumerService:0:Location"
        ));

        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(2))
            .body("'_id'", hasItems("2", "3"))
            .body("data.entityid", hasItems(
                "http://mock-sp",
                "https://profile.test2.surfconext.nl/authentication/metadata"))
            .body("data.metaDataFields.'name:en'", hasItems(
                "OpenConext Profile",
                "OpenConext Mujina SP"))
            .body("data.metaDataFields.'AssertionConsumerService:0:Location'", hasItems(
                "https://profile.test2.surfconext.nl/authentication/consume-assertion",
                "https://mujina-sp.test2.surfconext.nl/saml/SSO"))
            .body("data.allowedall", hasItems(
                true));

    }

    @Test
    public void whiteListing() throws Exception {
        given()
            .when()
            .get("manage/api/client/whiteListing/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(5))
            .body("data.allowedall", hasItems(true, false));
    }

    @Test
    public void validate() throws java.io.IOException {
        String json = readFile("/json/valid_service_provider.json");
        Map data = objectMapper.readValue(json, Map.class);
        MetaData metaData = new MetaData(EntityType.SP.getType(), data);
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/internal/validate/metadata")
            .then()
            .statusCode(SC_OK)
            .body(isEmptyOrNullString());
    }

    @Test
    public void validateWithErrors() throws java.io.IOException {
        String json = readFile("/json/valid_service_provider.json");
        Map data = objectMapper.readValue(json, Map.class);
        Map.class.cast(data.get("metaDataFields")).put("AssertionConsumerService:0:Binding", "bogus");
        MetaData metaData = new MetaData(EntityType.SP.getType(), data);
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/internal/validate/metadata")
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("validations", equalTo("#/metaDataFields/AssertionConsumerService:0:Binding: bogus is not a valid " +
                "enum value"));
    }
}