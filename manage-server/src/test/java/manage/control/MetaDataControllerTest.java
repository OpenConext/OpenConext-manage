package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import lombok.SneakyThrows;
import manage.AbstractIntegrationTest;
import manage.model.*;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.query.Query;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static io.restassured.config.XmlConfig.xmlConfig;
import static java.util.Collections.singletonMap;
import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;
import static manage.service.MetaDataService.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class MetaDataControllerTest extends AbstractIntegrationTest {

    @Test
    public void get() {
        given()
            .when()
            .get("manage/api/client/metadata/saml20_sp/1")
            .then()
            .statusCode(SC_OK)
            .body("id", equalTo("1"))
            .body("revision.number", equalTo(0))
            .body("data.metaDataFields.'name:pt'", equalTo("OpenConext PT"))
            .body("data.entityid", equalTo("Duis ad do"));
    }

    @Test
    public void getNotFound() {
        given()
            .when()
            .get("manage/api/client/metadata/saml20_sp/x")
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void templateSp() {
        given()
            .when()
            .get("manage/api/client/template/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("type", equalTo("saml20_sp"))
            .body("data.arp.enabled", equalTo(false))
            .body("data.arp.attributes", notNullValue())
            .body("data.metaDataFields", notNullValue())
            .body("data.entityid", emptyOrNullString())
            .body("data.state", equalTo("testaccepted"))
            .body("data.allowedEntities.size()", is(0))
            .body("data.disableConsent", emptyOrNullString());
    }

    @Test
    public void templateIdp() {
        given()
            .when()
            .get("manage/api/client/template/saml20_idp")
            .then()
            .statusCode(SC_OK)
            .body("type", equalTo("saml20_idp"))
            .body("data.allowedall", equalTo(true))
            .body("data.metaDataFields", notNullValue())
            .body("data.entityid", emptyOrNullString())
            .body("data.state", equalTo("testaccepted"))
            .body("data.allowedEntities.size()", is(0))
            .body("data.disableConsent.size()", is(0));
    }

    @Test
    public void templateOrganisation() {
        given()
            .when()
            .get("manage/api/client/template/organisation")
            .then()
            .statusCode(SC_OK)
            .body("type", equalTo("organisation"))
            .body("data.metaDataFields", notNullValue())
            .body("data.revisionnote", emptyOrNullString())
            .body("data.name", emptyOrNullString())
            .body("data.kvkNumber", emptyOrNullString())
            .body("data.notes", emptyOrNullString());
    }

    @Test
    public void remove() {
        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        assertNotNull(metaData);

        MetaDataChangeRequest changeRequest = this.doCreateChangeRequest();
        MetaData byId = metaDataRepository.findById(changeRequest.getId(), EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX));

        assertEquals(changeRequest.getId(), byId.getId());

        String reasonForDeletion = "Reason for deletion";
        given()
            .when()
            .header("Content-type", "application/json")
            .body(Collections.singletonMap("revisionNote", reasonForDeletion))
            .put("manage/api/client/metadata/saml20_sp/1")
            .then()
            .statusCode(SC_OK);

        metaData = metaDataRepository.findById("1", "saml20_sp");
        assertNull(metaData);

        byId = metaDataRepository.findById(changeRequest.getId(), EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX));
        assertNull(byId);

        List<MetaData> revisions = metaDataRepository.findRaw("saml20_sp_revision", "{\"revision.parentId\": \"1\"}");
        assertEquals(2, revisions.size());

        MetaData revision = revisions.get(1);
        assertEquals(reasonForDeletion, revision.getData().get("revisionnote"));
        assertEquals("saml2_user.com", revision.getRevision().getUpdatedBy());
    }

    @Test
    public void removeNonExistent() {
        given()
            .when()
            .header("Content-type", "application/json")
            .put("manage/api/client/metadata/saml20_sp/99999")
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void configuration() {
        given()
            .when()
            .get("manage/api/client/metadata/configuration")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(9))
            .body("title", hasItems("saml20_sp", "saml20_idp", "single_tenant_template", "oidc10_rp",
                "oauth20_rs", "organisation", "policy", "provisioning", "sram"));
    }

    @Test
    public void post() throws IOException {
        doPost(true, "saml2_user.com");
    }

    @Test
    public void postInternal() throws IOException {
        String id = doPost(false, "sp-portal");
        MetaData savedMetaData = metaDataRepository.findById(id, EntityType.SP.getType());
        savedMetaData.metaDataFields().put("name:en", "changedName");
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(savedMetaData)
            .header("Content-type", "application/json")
            .put("manage/api/internal/metadata")
            .then()
            .statusCode(SC_OK)
            .extract().path("id");
        MetaData updatedMetaData = metaDataRepository.findById(id, EntityType.SP.getType());
        assertEquals("changedName", updatedMetaData.metaDataFields().get("name:en"));
    }

    private String doPost(boolean client, String expectedUpdatedBy) throws java.io.IOException {
        String id = createServiceProviderMetaData(client);

        MetaData savedMetaData = metaDataRepository.findById(id, EntityType.SP.getType());
        Revision revision = savedMetaData.getRevision();
        assertEquals(expectedUpdatedBy, revision.getUpdatedBy());
        assertEquals(0, revision.getNumber());
        assertNotNull(revision.getCreated());

        return id;
    }

    @SneakyThrows
    private String createServiceProviderMetaData(boolean client) {
        Map<String, Object> data = readValueFromFile("/json/valid_service_provider.json");
        MetaData metaData = new MetaData(EntityType.SP.getType(), data);
//        System.out.println(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaData));
        RequestSpecification given = given();
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
    public void update() throws IOException {
        String id = createServiceProviderMetaData(true);
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.description:en", "New description");
        pathUpdates.put("allowedall", false);
        pathUpdates.put("allowedEntities", Arrays.asList(singletonMap("name", "https://allow-me"),
            singletonMap("name", "http://mock-idp")));
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate(id, EntityType.SP.getType(), pathUpdates, Collections.emptyMap());

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
            .body("data.allowedEntities[0].name", equalTo("http://mock-idp"))
            .body("data.allowedEntities", hasSize(1));
    }

    @Test
    public void updateArp() throws IOException {
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("arp", Map.of("enabled", true,
            "attributes",
            singletonMap("urn:mace:dir:attribute-def:eduPersonAffiliation",
                Collections.singletonList(Map.of("value", "student", "source", "idp")))));
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate("1", EntityType.SP.getType(), pathUpdates, Collections.emptyMap());

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .body(metaDataUpdate)
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_OK);

        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        Map<String, Object> arp = (Map<String, Object>) metaData.getData().get("arp");
        Map<String, Object> attributes = (Map<String, Object>) arp.get("attributes");
        assertEquals(1, attributes.size());
    }

    @Test
    public void updateAllowedEntities() {
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("allowedEntities", List.of(Map.of("name", "http://mock-idp")));
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate("3", EntityType.SP.getType(), pathUpdates, Collections.emptyMap());

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .body(metaDataUpdate)
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_OK);

        MetaData metaData = metaDataRepository.findById("3", EntityType.SP.getType());
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) metaData.getData().get("allowedEntities");
        assertEquals(1, allowedEntities.size());
    }

    @Test
    public void removeMetadataField() {
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.description:en", null);
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate("4", EntityType.SP.getType(), pathUpdates, Collections.emptyMap());

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .body(metaDataUpdate)
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_OK);

        MetaData metaData = metaDataRepository.findById("4", EntityType.SP.getType());
        Map<String, Object> metaDataFields = metaData.metaDataFields();
        assertFalse(metaDataFields.containsKey("description:en"));
    }


    @Test
    public void updateNonExistent() {
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate("99999", EntityType.SP.getType(), Collections.emptyMap(), Collections.emptyMap());

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .body(metaDataUpdate)
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void updateWithValidationErrors() throws IOException {
        String id = createServiceProviderMetaData(true);
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.NameIDFormats:0", "bogus");
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate(id, EntityType.SP.getType(), pathUpdates, Collections.emptyMap());

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
    public void updateWithoutCorrectScope() {
        given()
            .auth()
            .preemptive()
            .basic("pdp", "secret")
            .body(new MetaDataUpdate("id", EntityType.SP.getType(), new HashMap<>(), Collections.emptyMap()))
            .header("Content-type", "application/json")
            .when()
            .put("/manage/api/internal/merge")
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void updateWithoutCorrectUser() {
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
    public void putClient() {
        doPut(true, "saml2_user.com");
    }

    @Test
    public void putInternal() {
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
        assertEquals("some.user", revision.getUpdatedBy());
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
    public void autoComplete() {
        given()
            .when()
            .queryParam("query", "mock")
            .get("manage/api/client/autocomplete/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("suggestions.size()", is(2))
            .body("suggestions.'_id'", hasItems("3", "5"))
            .body("suggestions.data.entityid", hasItems(
                "http://mock-sp",
                "https://serviceregistry.test2.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp-2"));
    }

    @Test
    public void autoCompleteEscaping() {
        given()
            .when()
            .queryParam("query", "(test)")
            .get("manage/api/client/autocomplete/saml20_idp")
            .then()
            .statusCode(SC_OK)
            .body("suggestions.size()", is(1))
            .body("suggestions.data.entityid", hasItems(
                "https://idp.test2.surfconext.nl"));
    }

    @Test
    public void autoAlternativesWildcards() {
        given()
            .when()
            .queryParam("query", "Duis do")
            .get("manage/api/client/autocomplete/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("suggestions.size()", is(1));
    }

    @Test
    public void uniqueEntityId() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("entityid", "https@//oidc.rp");
        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/uniqueEntityId/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(1));
    }

    @Test
    public void uniqueEntityIdIncludingSRAM() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("entityid", "https@//oidc.rp");
        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/uniqueEntityId/sram")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(1));
    }

    @Test
    public void uniqueEntityIdCaseInsensitive() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("entityid", "https@//OIDC.RP");
        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/uniqueEntityId/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(1));
    }

    @Test
    public void search() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("metaDataFields.coin:do_not_add_attribute_aliases", true);
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
                "https://mujina-sp.test2.surfconext.nl/saml/SSO"));
    }

    @Test
    public void getAllEntities() {
        given()
            .when()
            .get("manage/api/client/metadata/allentities")
            .then()
            .statusCode(SC_OK)
            .body("suggestions.size()", is((int)metaDataList.stream()
                .filter(m -> List.of("oidc10_rp", "saml20_sp").contains(m.getType()))
                .count()));
    }

    /**
     * This is an outstanding bug where Manage cast numeric strings to int. Won't be fixed to
     * maintain backward compatibility for scrips that assume all metadata values are strings.
     */
    @Test
    public void searchBug() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("metaDataFields.coin:institution_id", 123);

        searchOptions.put(REQUESTED_ATTRIBUTES, Arrays.asList(
            "allowedall", "metaDataFields.AssertionConsumerService:0:Location"
        ));

        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_idp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(0));
    }

    @Test
    public void fieldIsStingAndSearchWithStingShouldReturnSomething() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("metaDataFields.coin:institution_id", "123");
        searchOptions.put(ALL_ATTRIBUTES, true);

        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(1));
    }

    @Test
    public void fieldIsStingAndSearchWithIntShouldReturnNothing() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("metaDataFields.coin:institution_id", 123);
        searchOptions.put(ALL_ATTRIBUTES, true);

        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(0));
    }


    @Test
    public void searchWithLogicalAnd() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("entityid", "Duis ad do");
        searchOptions.put("metaDataFields.AssertionConsumerService:0:Location",
            "https://profile.test2.surfconext.nl/authentication/consume-assertion");

        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(0));
    }

    @Test
    public void searchWithLogicalOr() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("entityid", "Duis ad do");
        searchOptions.put("metaDataFields.AssertionConsumerService:0:Location",
            "https://profile.test2.surfconext.nl/authentication/consume-assertion");

        searchOptions.put(LOGICAL_OPERATOR_IS_AND, false);

        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(2))
            .body("data.entityid", hasItems(
                "Duis ad do",
                "https://profile.test2.surfconext.nl/authentication/metadata"));
    }


    @Test
    public void searchWithListIn() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("entityid", Arrays.asList(
            "Duis ad do",
            "https://profile.test2.surfconext.nl/authentication/metadata",
            "http://mock-sp"));
        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(3));
    }

    @Test
    public void searchOidcRp() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("entityid", "https@//oidc.rp");
        searchOptions.put(ALL_ATTRIBUTES, true);

        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/oidc10_rp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(1));
    }

    @Test
    public void rawSearch() {
        String query = "{$and: [{$or:[{\"data.allowedEntities.name\": {$in: [\"http://mock-idp\"]}}, {\"data" +
            ".allowedall\": true}]}, {\"data.state\":\"prodaccepted\"}]}";
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .header("Content-type", "application/json")
            .queryParam("query", query)
            .get("manage/api/internal/rawSearch/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(5));
    }

    @Test
    public void rawSearchEncoded() throws UnsupportedEncodingException {
        String query = URLEncoder.encode("{$and: [{$or:[{\"data.allowedEntities.name\": {$in: " +
            "[\"http://mock-idp\"]}}, {\"data" +
            ".allowedall\": true}]}, {\"data.state\":\"prodaccepted\"}]}", "UTF-8");
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .header("Content-type", "application/json")
            .get("manage/api/internal/rawSearch/saml20_sp?query=" + query)
            .then()
            .statusCode(SC_OK)
            .body("size()", is(5));
    }

    @Test
    public void rawSearchPost() {
        String query = "{$and: [{$or:[{\"data.allowedEntities.name\": {$in: [\"http://mock-idp\"]}}, {\"data" +
            ".allowedall\": true}]}, {\"data.state\":\"prodaccepted\"}]}";
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .header("Content-type", "application/json")
            .body(query)
            .post("manage/api/internal/rawSearch/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(5));
    }

    private void doUpdate(EntityType type, String id, String revisionNote) {
        MetaData metaData = given()
            .when()
            .get("manage/api/client/metadata/" + type.getType() + "/" + id)
            .as(MetaData.class);
        metaData.getData().put("revisionnote", revisionNote);
        metaData.getData().put("notes", UUID.randomUUID().toString());
        given().when()
            .body(metaData)
            .header("Content-type", "application/json")
            .put("/manage/api/client/metadata")
            .then()
            .statusCode(SC_OK);
    }

    @Test
    public void recentActivity() {
        doDelete(EntityType.SP, "2", "Delete revision SP 2");
        doDelete(EntityType.RS, "10", "Delete revision RS 10");

        doUpdate(EntityType.SP, "1", "First revision SP");
        doUpdate(EntityType.RP, "9", "First revision RP");
        doUpdate(EntityType.IDP, "6", "First revision IDP");
        doUpdate(EntityType.SP, "1", "Second revision SP");
        doUpdate(EntityType.SP, "1", "Third revision SP");

        Map<String, Object> body = new HashMap<>();
        body.put("types", Arrays.asList(EntityType.RP.getType(), EntityType.IDP.getType(), EntityType.SP.getType(), EntityType.RS.getType()));
        body.put("limit", 6);
        List<Map<String, Object>> results = given()
            .when()
            .header("Content-type", "application/json")
            .body(body)
            .post("manage/api/client/recent-activity")
            .as(mapListTypeRef);

        assertEquals(6, results.size());

        Map<String, Object> sp1 = results.get(0);
        assertEquals("1", sp1.get("id"));
        assertEquals("Third revision SP", ((Map) sp1.get("data")).get("revisionnote"));
        assertNull(((Map) sp1.get("revision")).get("terminated"));

        Map<String, Object> idp6 = results.get(1);
        assertEquals("6", idp6.get("id"));
        assertEquals("First revision IDP", ((Map) idp6.get("data")).get("revisionnote"));
        assertNull(((Map) idp6.get("revision")).get("terminated"));

        Map<String, Object> rp9 = results.get(2);
        assertEquals("9", rp9.get("id"));
        assertEquals("First revision RP", ((Map) rp9.get("data")).get("revisionnote"));
        assertNull(((Map) rp9.get("revision")).get("terminated"));
        assertEquals(2, rp9.get("version"));
        assertEquals(2, ((Map) rp9.get("revision")).get("number"));

        Map<String, Object> rs10 = results.get(3);
        assertEquals("Delete revision RS 10", ((Map) rs10.get("data")).get("revisionnote"));
        assertNotNull(((Map) rs10.get("revision")).get("terminated"));

        Map<String, Object> sp2 = results.get(4);
        assertEquals("Delete revision SP 2", ((Map) sp2.get("data")).get("revisionnote"));
        assertNotNull(((Map) sp2.get("revision")).get("terminated"));
    }

    @Test
    public void validateUniqueFieldUnique() {
        given()
            .when()
            .get("manage/api/client/metadata/validate-unique-field/organisation/name/uniquename")
            .then()
            .statusCode(SC_OK);
    }

    @Test
    public void validateUniqueFieldNotUnique() {
        given()
            .when()
            .get("manage/api/client/metadata/validate-unique-field/organisation/name/Stichting Kennisnet")
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("exception", equalTo("manage.exception.ValueNotUniqueException"));
    }

    private void doDelete(EntityType entityType, String id, String revisionNote) {
        given().when()
            .header("Content-type", "application/json")
            .body(Collections.singletonMap("revisionNote", revisionNote))
            .put("manage/api/client/metadata/" + entityType.getType() + "/" + id)
            .then()
            .statusCode(SC_OK);
    }

    @Test
    public void searchWithAllAttributes() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put(ALL_ATTRIBUTES, true);

        given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(7))
            .body("data.metaDataFields.'SingleLogoutService_Location'", hasItems(
                "https://sls", null, null, null, null, null));
    }

    @Test
    public void whiteListingProdAccepted() {
        given()
            .when()
            .queryParam("state", "prodaccepted")
            .get("manage/api/client/whiteListing/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(6))
            .body("data.allowedall", hasItems(true, false));
    }

    @Test
    public void whiteListingTestAccepted() {
        given()
            .when()
            .queryParam("state", "testaccepted")
            .get("manage/api/client/whiteListing/saml20_sp")
            .then()
            .statusCode(SC_OK)
            .body("size()", is(2))
            .body("data.allowedall", hasItems(true, false));
    }

    @Test
    public void relyingParties() {
        List<Map<String, Object>> relyingParties = given()
            .when()
            .queryParam("resourceServerEntityID", "https@//oidc.rp.resourceServer")
            .get("manage/api/client/relyingParties")
            .as(mapListTypeRef);
        assertEquals(1, relyingParties.size());
    }

    @Test
    public void spPolicies() {
        List<Map<String, Object>> policies = given()
            .when()
            .queryParam("entityId", "https@//oidc.rp")
            .get("manage/api/client/spPolicies")
            .as(mapListTypeRef);
        assertEquals(1, policies.size());
        Map<String, Object> data = (Map<String, Object>) policies.get(0).get("data");
        assertTrue(data.containsKey("identityProviderIds"));
        assertTrue(data.containsKey("serviceProviderIds"));
    }

    @Test
    public void idpPolicies() {
        List<Map<String, Object>> policies = given()
            .when()
            .queryParam("entityId", "http://mock-idp")
            .get("manage/api/client/idpPolicies")
            .as(mapListTypeRef);
        assertEquals(1, policies.size());
    }

    @Test
    public void provisioning() {
        List<Map<String, Object>> applications = given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .contentType(ContentType.JSON)
            .body(List.of("1"))
            .when()
            .post("manage/api/internal/provisioning")
            .as(mapListTypeRef);
        assertEquals(1, applications.size());
        assertEquals("1", ((Map) ((List) ((Map) applications.get(0).get("data")).get("applications")).get(0)).get("id"));
    }

    @Test
    public void validate() throws java.io.IOException {
        Map<String, Object> data = readValueFromFile("/json/valid_service_provider.json");
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
            .body(emptyOrNullString());
    }

    @Test
    public void validateWithErrors() throws java.io.IOException {
        Map<String, Object> data = readValueFromFile("/json/valid_service_provider.json");
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

    @Test
    public void newSp() {
        String xml = readFile("sp_portal/sp_xml.xml");
        Map<String, String> body = singletonMap("xml", xml);

        doNewSp(body)
            .statusCode(SC_OK)
            .body("data.entityid", equalTo("https://engine.test2.surfconext.nl/authentication/sp/metadata"));
    }

    @Test
    public void newSpWithDuplicateEntityId() {
        String xml = readFile("sp_portal/sp_xml.xml");
        String entityId = "https://profile.test2.surfconext.nl/authentication/metadata";
        xml = xml.replaceAll(Pattern.quote("https://engine.test2.surfconext.nl/authentication/sp/metadata"),
            entityId);

        Map<String, String> body = new HashMap<>();
        body.put("xml", xml);
        Map<String, Object> res = given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .header("Content-type", "application/json")
            .body(body)
            .post("/manage/api/internal/new-sp")
            .as(new TypeRef<>() {
            });
        assertEquals(400, res.get("status"));
        assertEquals(ValidationException.class.getName(), res.get("error"));
    }

    @Test
    public void newSpWithMissingRequiredFields() {
        String xml = readFile("sp_portal/sp_xml_missing_required_fields.xml");
        Map<String, String> body = new HashMap<>();
        body.put("xml", xml);

        doNewSp(body)
            .statusCode(SC_BAD_REQUEST)
            .body("validations", equalTo(
                "#/metaDataFields: required key [AssertionConsumerService:0:Binding] not found, " +
                    "#/metaDataFields: required key [AssertionConsumerService:0:Location] not found"));
    }

    private ValidatableResponse doNewSp(Map<String, String> body) {
        return given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .body(body)
            .header("Content-type", "application/json")
            .when()
            .post("/manage/api/internal/new-sp")
            .then();
    }

    @Test
    public void updateSp() {
        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        String xml = readFile("sp_portal/sp_xml.xml");
        Map<String, String> body = singletonMap("xml", xml);
        doUpdateSp(body, metaData)
            .statusCode(SC_OK);

        MetaData updated = metaDataRepository.findById("1", EntityType.SP.getType());
        assertEquals(1L, updated.getVersion().longValue());
        assertEquals("https://engine.test2.surfconext.nl/authentication/sp/metadata",
            updated.getData().get("entityid"));
    }

    @Test
    public void exportToXml() {
        String xml = given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .get("/manage/api/internal/sp-metadata/1")
            .asString();
        assertTrue(xml.contains("entityID=\"Duis ad do\""));
    }

    @Test
    public void putReconcileEntityIdIdP() {
        MetaData metaData = metaDataRepository.findById("7", EntityType.IDP.getType());
        Map.class.cast(metaData.getData()).put("entityid", "new-entityid");
        given()
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .put("/manage/api/client/metadata")
            .then()
            .statusCode(SC_OK);

        List<MetaData> sps = metaDataRepository.findRaw("saml20_sp", "{\"data.allowedEntities.name\" : " +
            "\"new-entityid\"}");
        assertEquals(2, sps.size());
    }

    @Test
    public void deleteReconcileEntityIdSP() {
        MetaData idp = metaDataRepository.findById("6", "saml20_idp");
        assertEquals(3, List.class.cast(idp.getData().get("allowedEntities")).size());
        assertEquals(2, List.class.cast(idp.getData().get("disableConsent")).size());

        given()
            .when()
            .header("Content-type", "application/json")
            .put("manage/api/client/metadata/saml20_sp/3")
            .then()
            .statusCode(SC_OK);

        idp = metaDataRepository.findById("6", "saml20_idp");
        assertEquals(2, List.class.cast(idp.getData().get("allowedEntities")).size());
        assertEquals(1, List.class.cast(idp.getData().get("disableConsent")).size());

    }

    @Test
    public void restoreRevision() {
        String type = EntityType.SP.getType();
        MetaData metaData = metaDataRepository.findById("1", type);
        Map.class.cast(metaData.getData()).put("entityid", "something changed");
        given()
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .put("/manage/api/client/metadata")
            .then()
            .statusCode(SC_OK);

        List<MetaData> revisions = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_sp_revision");
        assertEquals(1, revisions.size());
        RevisionRestore revisionRestore = new RevisionRestore(revisions.get(0).getId(),
            type.concat("_revision"), type);

        given()
            .when()
            .body(revisionRestore)
            .header("Content-type", "application/json")
            .put("manage/api/client/restoreRevision")
            .then()
            .statusCode(SC_OK);

        revisions = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_sp_revision");
        assertEquals(2, revisions.size());
        revisions.forEach(rev -> assertEquals("1", rev.getRevision().getParentId()));
    }

    @Test
    public void importFeed() throws IOException {
        String urlS = new ClassPathResource("xml/edugain_feed.xml").getURL().toString();
        Import importRequest = new Import(urlS, null);
        Map result = given()
            .body(importRequest)
            .header("Content-type", "application/json")
            .post("manage/api/client/import/feed")
            .getBody()
            .as(Map.class);
        assertEquals(3, result.size());
        assertEquals(2, List.class.cast(result.get("imported")).size());

        result = given()
            .body(importRequest)
            .header("Content-type", "application/json")
            .post("manage/api/client/import/feed")
            .getBody()
            .as(Map.class);
        assertEquals(3, result.size());
        assertEquals(2, List.class.cast(result.get("no_changes")).size());

        urlS = new ClassPathResource("xml/edugain_feed_changed.xml").getURL().toString();
        importRequest = new Import(urlS, null);
        result = given()
            .body(importRequest)
            .header("Content-type", "application/json")
            .post("manage/api/client/import/feed")
            .getBody()
            .as(Map.class);
        assertEquals(3, result.size());
        assertEquals(2, List.class.cast(result.get("merged")).size());
    }

    @Test
    public void importFeedIdemPotency() throws IOException {
        String urlS = new ClassPathResource("import_xml/edugain_sniplet.xml").getURL().toString();
        Import importRequest = new Import(urlS, null);
        Map result = given()
            .body(importRequest)
            .header("Content-type", "application/json")
            .post("manage/api/client/import/feed")
            .getBody()
            .as(Map.class);

        assertEquals(1, ((List) result.get("total")).get(0));
        Map imported = (Map) ((List) result.get("imported")).get(0);
        assertEquals("https://impacter.eu/sso/metadata", imported.get("entityId"));

        String id = (String) imported.get("id");
        MetaData metaData = given()
            .body(importRequest)
            .header("Content-type", "application/json")
            .get("manage/api/client/metadata/saml20_sp/" + id)
            .getBody()
            .as(MetaData.class);
        Map<String, Object> metaDataFields = metaData.metaDataFields();
        assertEquals("eduGAIN", metaDataFields.get("coin:interfed_source"));
        assertEquals(true, metaDataFields.get("coin:imported_from_edugain"));

        importRequest = new Import(urlS, "https://impacter.eu/sso/metadata");
        result = given()
            .body(importRequest)
            .header("Content-type", "application/json")
            .post("manage/api/client/import/endpoint/xml/saml20_sp")
            .getBody()
            .as(Map.class);
        assertEquals("eduGAIN", ((Map) result.get("metaDataFields")).get("coin:interfed_source"));
    }


    @Test
    public void restoreDeletedRevision() {
        String type = EntityType.SP.getType();
        given()
            .when()
            .header("Content-type", "application/json")
            .put("manage/api/client/metadata/saml20_sp/1")
            .then()
            .statusCode(SC_OK);

        MetaData metaData = metaDataRepository.findById("1", "saml20_sp");
        assertNull(metaData);

        List<MetaData> revisions = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_sp_revision");
        assertEquals(2, revisions.size());
        RevisionRestore revisionRestore = new RevisionRestore(revisions.get(0).getId(),
            type.concat("_revision"), type);

        given()
            .when()
            .body(revisionRestore)
            .header("Content-type", "application/json")
            .put("manage/api/client/restoreDeleted")
            .then()
            .statusCode(SC_OK);

        metaData = metaDataRepository.findById("1", "saml20_sp");
        assertNotNull(metaData);

        revisions = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_sp_revision");
        assertEquals(2, revisions.size());
        revisions.forEach(rev -> assertEquals("1", rev.getRevision().getParentId()));
    }


    private ValidatableResponse doUpdateSp(Map<String, String> body, MetaData metaData) {
        return given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .body(body)
            .header("Content-type", "application/json")
            .when()
            .post("/manage/api/internal/update-sp/" + metaData.getId() + "/" + metaData.getVersion())
            .then();
    }

    @Test
    public void updateWithValidationError() throws IOException {
        Map<String, Object> data = readValueFromFile("/metadata_templates/oidc10_rp.template.json");

        data.put("entityid", "https://unique_entity_id");

        List.class.cast(Map.class.cast(data.get("metaDataFields")).get("redirectUrls")).add("javascript:alert(document.domain)");

        MetaData metaData = new MetaData(EntityType.RP.getType(), data);
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/client/metadata")
            .then()
            .statusCode(SC_BAD_REQUEST);
    }

    @Test
    public void deleteMetaDataKey() {
        String keyToDelete = "displayName:en";
        List result = given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(new MetaDataKeyDelete("saml20_sp", keyToDelete))
            .header("Content-type", "application/json")
            .put("manage/api/internal/delete-metadata-key")
            .getBody()
            .as(List.class);

        assertEquals(5, result.size());
        result.forEach(entityId -> {
            MetaData metaData = metaDataRepository.findRaw("saml20_sp",
                String.format("{\"data.entityid\":\"%s\"}", entityId)).get(0);
            assertFalse(metaData.metaDataFields().containsKey(keyToDelete));
        });
    }

    @Test
    public void connectValidSpWithoutInteraction() {
        String idpEntityId = "https://idp.test2.surfconext.nl";
        String idUId = "6";
        String spId = "Duis ad do";
        String spType = "saml20_sp";
        Map<String, String> connectionData = new HashMap<>();
        connectionData.put("idpId", idpEntityId);
        connectionData.put("spId", spId);
        connectionData.put("spType", spType);
        connectionData.put("user", "John Doe");
        connectionData.put("loaLevel", "http://test.surfconext.nl/assurance/loa2");

        given()
            .auth()
            .preemptive()
            .basic("dashboard", "secret")
            .header("Content-type", "application/json")
            .body(connectionData)
            .put("manage/api/internal/connectWithoutInteraction")
            .then()
            .statusCode(SC_OK);

        MetaData idp = metaDataRepository.findById(idUId, EntityType.IDP.getType());
        Map<String, Object> data = idp.getData();
        List<Map> allowedEntities = (List<Map>) data.get("allowedEntities");

        assertTrue(listOfMapsContainsValue(allowedEntities, spId));

        List<Map<String, String>> stepUpEntities = (List<Map<String, String>>) data.get("stepupEntities");
        String level = stepUpEntities.stream().filter(map -> map.get("name").equals(spId)).map(map -> map.get("level")).findFirst().get();
        assertEquals("http://test.surfconext.nl/assurance/loa2", level);
    }

    @Test
    public void connectSpWithSameInstitutionGUIDAsIdp() {
        String idpEntityId = "https://idp.test2.surfconext.nl";
        String idUId = "6";
        String rpId = "https@//oidc.rp";
        String rpType = "oidc10_rp";
        Map<String, String> connectionData = new HashMap<>();
        connectionData.put("idpId", idpEntityId);
        connectionData.put("spId", rpId);
        connectionData.put("spType", rpType);
        connectionData.put("user", "John Doe");
        connectionData.put("userUrn", "urn:collab:person:example.com:jdoe");

        given()
            .auth()
            .preemptive()
            .basic("dashboard", "secret")
            .header("Content-type", "application/json")
            .body(connectionData)
            .put("manage/api/internal/connectWithoutInteraction")
            .then()
            .statusCode(SC_OK);

        MetaData idp = metaDataRepository.findById(idUId, EntityType.IDP.getType());
        Map<String, Object> data = idp.getData();
        assertEquals("Connected https@//oidc.rp on request of John Doe - urn:collab:person:example.com:jdoe via Dashboard.", data.get("revisionnote"));

        List<Map> allowedEntities = (List<Map>) data.get("allowedEntities");

        assert listOfMapsContainsValue(allowedEntities, rpId);
    }

    @Test
    public void connectSpWithIdPNotAllowed() {
        String idpEntityId = "https://idp.test2.surfconext.nl";
        String spId = "https://serviceregistry.test2.surfconext.nl/simplesaml/module.php/saml/sp/metadata.php/default-sp-2";
        String spType = "saml20_sp";
        Map<String, String> connectionData = new HashMap<>();
        connectionData.put("idpId", idpEntityId);
        connectionData.put("spId", spId);
        connectionData.put("spType", spType);
        connectionData.put("user", "John Doe");

        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .header("Content-type", "application/json")
            .body(connectionData)
            .put("manage/api/internal/connectWithoutInteraction")
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void connectSpAllowsNoneWithoutInteraction() {
        Map<String, String> connectionData = new HashMap<>();
        String idpEntityId = "https://idp.test2.surfconext.nl";
        connectionData.put("idpId", idpEntityId);
        connectionData.put("spId", "https://profile.test2.surfconext.nl/authentication/metadata");
        connectionData.put("spType", "saml20_sp");
        connectionData.put("user", "John Doe");

        given()
            .auth()
            .preemptive()
            .basic("dashboard", "secret")
            .header("Content-type", "application/json")
            .body(connectionData)
            .put("manage/api/internal/connectWithoutInteraction")
            .then()
            .statusCode(SC_OK);

        MetaData sp = metaDataRepository.findById("2", EntityType.SP.getType());
        Map<String, Object> data = sp.getData();
        List<Map> allowedEntities = (List<Map>) data.get("allowedEntities");

        assert listOfMapsContainsValue(allowedEntities, idpEntityId);
    }

    @Test
    public void connectInvalidSpWithoutInteraction() {
        Map<String, String> connectionData = new HashMap<>();
        connectionData.put("idpId", "Duis ad do");
        connectionData.put("spId", null);
        connectionData.put("spType", "saml20_sp");
        connectionData.put("user", "John Doe");

        given()
            .auth()
            .preemptive()
            .basic("dashboard", "secret")
            .header("Content-type", "application/json")
            .body(connectionData)
            .put("manage/api/internal/connectWithoutInteraction/")
            .then()
            .statusCode(SC_NOT_FOUND);
    }

    @Test
    public void connectValidSpWithoutInteractionInvalidApiUser() {
        String idpEntityId = "https://idp.test2.surfconext.nl";
        String spId = "Duis ad do";
        String spType = "saml20_sp";
        Map<String, String> connectionData = new HashMap<>();
        connectionData.put("idpId", idpEntityId);
        connectionData.put("spId", spId);
        connectionData.put("spType", spType);
        connectionData.put("user", "John Doe");
        connectionData.put("loaLevel", "http://test.surfconext.nl/assurance/loa2");

        given()
            .auth()
            .preemptive()
            .basic("stats", "secret")
            .header("Content-type", "application/json")
            .body(connectionData)
            .put("manage/api/internal/connectWithoutInteraction")
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void exportMetadataXml() throws IOException {
        given()
            .config(newConfig().xmlConfig(xmlConfig()
                .declareNamespace("md", "urn:oasis:names:tc:SAML:2.0:metadata")))
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .contentType(ContentType.XML)
            .when()
            .get("/manage/api/internal/xml/metadata/saml20_sp/1")
            .then()
            .body("md:EntityDescriptor.@entityID",
                equalTo("Duis ad do"));
    }

    @Test
    public void emptyArpArray() throws java.io.IOException {
        Map<String, Object> data = readValueFromFile("/json/sp_dashboard_arp_array.json");
        MetaData metaData = new MetaData(EntityType.SP.getType(), data);
        given().auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/internal/metadata")
            .then()
            .statusCode(SC_BAD_REQUEST)
            .body("validations", equalTo("#/arp/attributes: expected type: JSONObject, found: JSONArray"));
    }

    @Test
    public void notAllowed() throws java.io.IOException {
        Map<String, Object> data = readValueFromFile("/json/valid_service_provider.json");
        MetaData metaData = new MetaData(EntityType.SP.getType(), data);
        given().auth()
            .preemptive()
            .basic("pdp", "secret")
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/internal/metadata")
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    @Test
    public void validationErrors() {
        Map<String, Object> data = readValueFromFile("/json/valid_service_provider.json");
        MetaData metaData = new MetaData(EntityType.SP.getType(), data);
        metaData.metaDataFields().clear();
        metaData.getData().put("entityid", UUID.randomUUID().toString());

        Map<String, Object> result = given().auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/internal/metadata")
            .as(new TypeRef<>() {
            });
        long nbrValidations = Stream.of(((String) result.get("validations")).split("#")).filter(s -> !s.trim().isEmpty()).count();
        assertEquals(3L, nbrValidations);

    }

    @Test
    public void createChangeRequest() throws JsonProcessingException {
        doCreateChangeRequest();

        List<MetaDataChangeRequest> requests = given()
            .when()
            .get("manage/api/client/change-requests/saml20_sp/1")
            .as(new TypeRef<>() {
            });
        assertEquals(1, requests.size());

        MetaDataChangeRequest request = requests.get(0);
        assertEquals(4, request.getMetaDataSummary().size());
        assertEquals(3, request.getAuditData().size());
        assertEquals(3, request.getPathUpdates().size());
    }

    @Test
    public void deleteChangeRequestAfterMetaDataDelete() {
        doCreateChangeRequest();
        List<MetaDataChangeRequest> requests = given()
            .when()
            .get("manage/api/client/change-requests/saml20_sp/1")
            .as(new TypeRef<>() {
            });
        assertEquals(1, requests.size());

        doDelete(EntityType.SP, "1", "Delete SP 1");

        requests = given()
            .when()
            .get("manage/api/client/change-requests/saml20_sp/1")
            .as(new TypeRef<>() {
            });
        assertEquals(0, requests.size());
    }

    @Test
    public void arpAdditionChangeRequest() {
        String changeRequestJson = readFile("json/incremental_arp_change_request.json");
        given().auth().preemptive().basic("sp-portal", "secret")
            .when()
            .body(changeRequestJson)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .then()
            .statusCode(SC_OK);
        MetaDataChangeRequest metaDataChangeRequest = mongoTemplate()
            .find(new Query(), MetaDataChangeRequest.class, EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX)).get(0);
        assertEquals("Change", metaDataChangeRequest.getRequestType());

        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest(metaDataChangeRequest.getId(), EntityType.SP.getType(), metaDataChangeRequest.getMetaDataId(), "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        Map attributes = (Map) ((Map) metaData.getData().get("arp")).get("attributes");

        assertTrue(attributes.containsKey("urn:mace:dir:attribute-def:eduPersonOrcid"));
        assertEquals(6, attributes.size());
    }

    @Test
    public void arpRemovalChangeRequest() {
        String changeRequestJson = readFile("json/incremental_arp_removal_change_request.json");
        given().auth().preemptive().basic("sp-portal", "secret")
            .when()
            .body(changeRequestJson)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .then()
            .statusCode(SC_OK);
        MetaDataChangeRequest metaDataChangeRequest = mongoTemplate()
            .find(new Query(), MetaDataChangeRequest.class, EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX)).get(0);
        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest(metaDataChangeRequest.getId(), EntityType.SP.getType(), metaDataChangeRequest.getMetaDataId(), "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        Map attributes = (Map) ((Map) metaData.getData().get("arp")).get("attributes");

        assertFalse(attributes.containsKey("urn:mace:dir:attribute-def:mail"));
        assertEquals(4, attributes.size());
    }

    @Test
    public void acceptChangeRequest() {
        doCreateChangeRequest();
        MetaDataChangeRequest metaDataChangeRequest = mongoTemplate()
            .find(new Query(), MetaDataChangeRequest.class, EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX)).get(0);
        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest(metaDataChangeRequest.getId(), EntityType.SP.getType(), metaDataChangeRequest.getMetaDataId(), "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        assertEquals("New description", metaData.metaDataFields().get("description:en"));

        List<MetaDataChangeRequest> requests = given()
            .when()
            .get("manage/api/client/change-requests/saml20_sp/1")
            .as(new TypeRef<>() {
            });
        assertEquals(0, requests.size());
    }

    @Test
    public void changeRequestDisableConsent() {
        Map<String, Object> pathUpdates = new HashMap<>();
        List<Map<String, String>> disableConsent = List.of(
            Map.of("name", "http://disableConsent1", "type", "default_consent", "explanation:nl", "NL", "explanation:en", "EN"),
            Map.of("name", "http://mock-sp", "type", "no_consent"),
            Map.of("name", "Duis ad do", "type", "minimal_consent", "explanation:nl", "NL", "explanation:en", "EN")
        );
        pathUpdates.put("disableConsent", disableConsent);

        Map<String, Object> auditData = new HashMap<>();
        auditData.put("user", "jdoe");

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest(
            "6", EntityType.IDP.getType(), "Because....", pathUpdates, auditData
        );
        Map results = given().auth().preemptive().basic("dashboard", "secret")
            .when()
            .body(changeRequest)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .as(Map.class);

        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest((String) results.get("id"), EntityType.IDP.getType(), "6", "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("6", EntityType.IDP.getType());
        List<Map<String, String>> newDisableConsent = (List<Map<String, String>>) metaData.getData().get("disableConsent");
        Map<String, String> duisAdDo = newDisableConsent.stream().filter(map -> map.get("name").equals("Duis ad do")).findFirst().get();
        assertEquals("minimal_consent", duisAdDo.get("type"));
    }

    @Test
    public void changeRequestRemoveMetadataField() {
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.contacts:3:contactType", null);

        Map<String, Object> auditData = new HashMap<>();
        auditData.put("user", "jdoe");

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest(
            "6", EntityType.IDP.getType(), "Because....", pathUpdates, auditData
        );
        Map results = given().auth().preemptive().basic("dashboard", "secret")
            .when()
            .body(changeRequest)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .as(Map.class);

        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest((String) results.get("id"), EntityType.IDP.getType(), "6", "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("6", EntityType.IDP.getType());
        assertFalse(metaData.metaDataFields().containsKey("contacts:3:contactType"));
    }

    @Test
    public void rejectChangeRequest() {
        doCreateChangeRequest();
        MetaDataChangeRequest metaDataChangeRequest = mongoTemplate()
            .find(new Query(), MetaDataChangeRequest.class, EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX)).get(0);
        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest(metaDataChangeRequest.getId(), EntityType.SP.getType(), metaDataChangeRequest.getMetaDataId(), "Rev notes"))
            .put("/manage/api/client/change-requests/reject")
            .then()
            .statusCode(200);
        List<MetaDataChangeRequest> requests = given()
            .when()
            .get("manage/api/client/change-requests/saml20_sp/1")
            .as(new TypeRef<>() {
            });
        assertEquals(0, requests.size());
    }

    @Test
    public void changeRequestAddition() {
        Map<String, Object> pathUpdates = Map.of("allowedEntities", Map.of("name", "https://idp.test2.surfconext.nl"));
        Map<String, Object> auditData = Map.of("user", "jdoe");

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest(
            "2", EntityType.SP.getType(), "Because....", pathUpdates, auditData
        );
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.ADDITION);

        Map results = given().auth().preemptive().basic("sp-portal", "secret")
            .when()
            .body(changeRequest)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .as(Map.class);

        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest((String) results.get("id"), EntityType.SP.getType(), "2", "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("2", EntityType.SP.getType());
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) metaData.getData().get("allowedEntities");
        assertEquals(2, allowedEntities.size());
    }

    @Test
    public void changeRequestRemoval() {
        Map<String, Object> pathUpdates = Map.of("allowedEntities", Map.of("name", "http://mock-idp"));
        Map<String, Object> auditData = Map.of("user", "jdoe");

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest(
            "2", EntityType.SP.getType(), "Because....", pathUpdates, auditData
        );
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.REMOVAL);

        Map results = given().auth().preemptive().basic("sp-portal", "secret")
            .when()
            .body(changeRequest)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .as(Map.class);

        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest((String) results.get("id"), EntityType.SP.getType(), "2", "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("2", EntityType.SP.getType());
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) metaData.getData().get("allowedEntities");
        assertEquals(0, allowedEntities.size());
    }

    @Test
    public void changeRequestRemovalMultiple() {
        MetaData currentMetaData = metaDataRepository.findById("6", EntityType.IDP.getType());
        List<Map<String, String>> currentAllowedEntities = (List<Map<String, String>>) currentMetaData.getData().get("allowedEntities");
        assertEquals(3, currentAllowedEntities.size());

        Map<String, Object> pathUpdates = Map.of("allowedEntities",
            List.of(Map.of("name", "https://oidc.test.client"), Map.of("name", "http://mock-sp")));
        Map<String, Object> auditData = Map.of("user", "jdoe");

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest(
            "6", EntityType.IDP.getType(), "Because....", pathUpdates, auditData
        );
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.REMOVAL);

        Map results = given().auth().preemptive().basic("dashboard", "secret")
            .when()
            .body(changeRequest)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .as(Map.class);

        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest((String) results.get("id"), EntityType.IDP.getType(), "6", "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("6", EntityType.IDP.getType());
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) metaData.getData().get("allowedEntities");
        assertEquals(0, allowedEntities.size());
    }

    @Test
    public void changeRequestChangeEntityID() {
        Map<String, Object> pathUpdates = Map.of("entityid", "https://changed.org");
        Map<String, Object> auditData = Map.of("user", "jdoe");

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest(
            "6", EntityType.IDP.getType(), "Because....", pathUpdates, auditData
        );
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.ADDITION);

        Map results = given().auth().preemptive().basic("dashboard", "secret")
            .when()
            .body(changeRequest)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .as(Map.class);

        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest((String) results.get("id"), EntityType.IDP.getType(), "6", "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("6", EntityType.IDP.getType());
        assertEquals("https://changed.org", metaData.getData().get("entityid"));
    }

    @Test
    public void searchWithEntityCategoriesMultipleKeys() throws JsonProcessingException {
        Map<String, Object> searchOptions = readValueFromFile("/api/search_multiple_equal_keys.json");

        List<Map<String, Object>> results = given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .as(new TypeRef<>() {
            });
        assertEquals(1, results.size());
    }

    @Test
    public void searchWithEntityCategoriesList() throws JsonProcessingException {
        Map<String, Object> searchOptions = readValueFromFile("/api/search_multiple_list_values.json");

        List<Map<String, Object>> results = given()
            .when()
            .body(searchOptions)
            .header("Content-type", "application/json")
            .post("manage/api/client/search/saml20_sp")
            .as(new TypeRef<>() {
            });
        assertEquals(3, results.size());
    }

    @Test
    public void flowStepUpPolicyMetaData() {
        Map<String, Object> data = readValueFromFile("/json/valid_policy_step.json");
        MetaData metaData = new MetaData(EntityType.PDP.getType(), data);
        MetaData newMetaData = given()
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/client/metadata")
            .as(MetaData.class);
        MetaData retrievedMetaData = given()
            .when()
            .get("manage/api/client/metadata/policy/" + newMetaData.getId())
            .as(MetaData.class);
        assertEquals(EntityType.PDP.getType(), retrievedMetaData.getType());

        retrievedMetaData.getData().put("revisionnote", "Update test");
        retrievedMetaData.getData().put("denyAdvice", "Changed");

        MetaData updatedMetaData = given()
            .when()
            .body(retrievedMetaData)
            .header("Content-type", "application/json")
            .put("manage/api/client/metadata")
            .as(MetaData.class);
        assertEquals(updatedMetaData.getId(), retrievedMetaData.getId());

        List<MetaData> revisions = given()
            .when()
            .pathParam("type", EntityType.PDP.getType())
            .pathParam("parentId", newMetaData.getId())
            .get("manage/api/client/revisions/{type}/{parentId}")
            .as(new TypeRef<>() {
            });
        assertEquals(1, revisions.size());
    }

    @Test
    public void flowRegPolicyMetaData() {
        Map<String, Object> data = readValueFromFile("/json/valid_policy_reg.json");
        ((List) data.get("identityProviderIds")).add(Map.of("name", "nope-not-an-idp"));
        ((List) data.get("serviceProviderIds")).add(Map.of("name", "nope-not-an-idp"));
        MetaData metaData = new MetaData(EntityType.PDP.getType(), data);
        MetaData newMetaData = given()
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/client/metadata")
            .as(MetaData.class);
        MetaData retrievedMetaData = given()
            .when()
            .get("manage/api/client/metadata/policy/" + newMetaData.getId())
            .as(MetaData.class);
        assertEquals(1, ((List) retrievedMetaData.getData().get("identityProviderIds")).size());
        assertEquals(1, ((List) retrievedMetaData.getData().get("serviceProviderIds")).size());
        assertEquals(EntityType.PDP.getType(), retrievedMetaData.getType());
        assertNotNull(retrievedMetaData.getId());
        assertEquals("reg", retrievedMetaData.getData().get("type"));
    }

    @Test
    public void policyMetaDataRegularValidation() {
        Map<String, Object> data = readValueFromFile("/metadata_templates/policy.template.json");
        MetaData metaData = new MetaData(EntityType.PDP.getType(), data);
        Map<String, Object> result = given()
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/client/metadata")
            .as(new TypeRef<>() {
            });
        long nbrValidations = Stream.of(((String) result.get("validations")).split("#:")).filter(s -> !s.trim().isEmpty()).count();
        assertEquals(3L, nbrValidations);
    }

    @Test
    public void policyMetaDataStepUpValidation() {
        Map<String, Object> data = readValueFromFile("/metadata_templates/policy.template.json");
        data.put("type", "step");
        MetaData metaData = new MetaData(EntityType.PDP.getType(), data);
        Map<String, Object> result = given()
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/client/metadata")
            .as(new TypeRef<>() {
            });
        long nbrValidations = Stream.of(((String) result.get("validations")).split("#:")).filter(s -> !s.trim().isEmpty()).count();
        assertEquals(1L, nbrValidations);
    }

    @Test
    public void acsChangeRequest() {
        String changeRequestJson = readFile("json/invalid_acs_change_request.json");
        given().auth().preemptive().basic("sp-portal", "secret")
            .when()
            .body(changeRequestJson)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .then()
            .statusCode(SC_OK);
        MetaDataChangeRequest metaDataChangeRequest = mongoTemplate()
            .find(new Query(), MetaDataChangeRequest.class, EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX)).get(0);
        given()
            .when()
            .contentType(ContentType.JSON)
            .body(new ChangeRequest(metaDataChangeRequest.getId(), EntityType.SP.getType(), metaDataChangeRequest.getMetaDataId(), "Rev notes"))
            .put("/manage/api/client/change-requests/accept")
            .then()
            .statusCode(200);

        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        Map<String, Object> metaDataFields = metaData.metaDataFields();

        assertEquals("https://print.saxion.nl/end-user/saml/SSO", metaDataFields.get("AssertionConsumerService:0:Location"));
        List.of("metaDataFields.AssertionConsumerService:1:Location",
                "metaDataFields.AssertionConsumerService:1:Binding",
                "metaDataFields.AssertionConsumerService:2:Location",
                "metaDataFields.AssertionConsumerService:2:Binding",
                "metaDataFields.AssertionConsumerService:3:Location",
                "metaDataFields.AssertionConsumerService:3:Binding")
            .forEach(key -> assertFalse(metaDataFields.containsKey(key)));

        assertTrue(((String) metaDataFields.get("certData")).startsWith("MIIG5jCCBM6gAwIBAgIBCDANBgkqhkiG9w0BA"));
    }

    @Test
    public void deleteServiceProviderWithScopeDeleteSP() {
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .pathParam("type", EntityType.SP.getType())
            .pathParam("id", "1")
            .delete("/manage/api/internal/metadata/{type}/{id}")
            .then()
            .statusCode(SC_OK);
        MetaData metaData = metaDataRepository.findById("1", EntityType.SP.getType());
        assertNull(metaData);
    }

    @Test
    public void deleteIdentityProviderWithScopeDeleteSPForbidden() {
        given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .pathParam("type", EntityType.IDP.getType())
            .pathParam("id", "6")
            .delete("/manage/api/internal/metadata/{type}/{id}")
            .then()
            .statusCode(SC_FORBIDDEN);
    }

    private MetaDataChangeRequest doCreateChangeRequest() {
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.description:en", "New description");
        pathUpdates.put("allowedall", false);
        pathUpdates.put("allowedEntities", Arrays.asList(singletonMap("name", "https://allow-me"),
            singletonMap("name", "http://mock-idp")));
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("user", "jdoe");

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest(
            "1", EntityType.SP.getType(), "Because....", pathUpdates, auditData
        );
        return given().auth().preemptive().basic("dashboard", "secret")
            .when()
            .body(changeRequest)
            .header("Content-type", "application/json")
            .post("manage/api/internal/change-requests")
            .as(MetaDataChangeRequest.class);
    }

    @Test
    public void saveInvalidARPMetaData() {
        MetaData existingMetaData = given()
            .when()
            .pathParam("id", "11")
            .get("manage/api/client/metadata/saml20_sp/{id}")
            .as(MetaData.class);
        Map<String, Object> arp = (Map<String, Object>) existingMetaData.getData().get("arp");
        Map<String, List<Map<String, Object>>> attributes = (Map<String, List<Map<String, Object>>>) arp.get("attributes");
        List<Map<String, Object>> givenNameArpAttributes = attributes.get("urn:mace:dir:attribute-def:givenName");
        Map<String, Object> arpAttribute = givenNameArpAttributes.get(0);
        arpAttribute.put("useAsNameId", true);
        arpAttribute.put("use_as_name_id", true);
        arpAttribute.put("releaseAs", true);

        Map errors = given()
            .when()
            .body(existingMetaData)
            .header("Content-type", "application/json")
            .put("manage/api/client/metadata")
            .as(Map.class);
        String validations = (String) errors.get("validations");
        assertEquals("#/arp/attributes/urn:mace:dir:attribute-def:givenName/0: extraneous key [use_as_name_id] is not permitted, " +
                "#/arp/attributes/urn:mace:dir:attribute-def:givenName/0: extraneous key [releaseAs] is not permitted, " +
                "#/arp/attributes/urn:mace:dir:attribute-def:givenName/0: extraneous key [useAsNameId] is not permitted",
            validations);
    }

    @Test
    public void saveWithNewValidationNameIDFormat() {
        Map<String, Object> data = readValueFromFile("/metadata_templates/saml20_idp.template.json");

        data.put("entityid", "https://unique_entity_id");
        Map.class.cast(data.get("metaDataFields")).put("NameIDFormat", "urn:oasis:names:tc:SAML:2.0:nameid-format:emailAddress");

        MetaData metaData = new MetaData(EntityType.IDP.getType(), data);
        Map<String, Object> results = given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/client/metadata")
            .as(new TypeRef<>() {
            });
        assertNotNull(results.get("id"));
        Map<String, Object> revision = (Map<String, Object>) results.get("revision");
        assertEquals(0, revision.get("number"));
    }

    @Test
    public void saveWithInvalidationNameIDFormat() {
        Map<String, Object> data = readValueFromFile("/metadata_templates/saml20_idp.template.json");

        data.put("entityid", "https://unique_entity_id");
        Map.class.cast(data.get("metaDataFields")).put("NameIDFormat", "urn:oasis:names:tc:SAML:2.0:nameid-format:bogus");

        MetaData metaData = new MetaData(EntityType.IDP.getType(), data);
        Map<String, Object> results = given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .body(metaData)
            .header("Content-type", "application/json")
            .post("manage/api/client/metadata")
            .as(new TypeRef<>() {
            });
        assertEquals("#/metaDataFields/NameIDFormat: urn:oasis:names:tc:SAML:2.0:nameid-format:bogus is not a valid enum value",
            results.get("validations"));
    }

    @Test
    public void removeExtraneousKeys() {
        MetaData metaData = metaDataRepository.findById("1", "saml20_sp");
        metaData.metaDataFields().put("extraneous_key", "bogus");
        metaData.metaDataFields().put("1_is_not_2", "bogus");
        metaDataRepository.update(metaData);

        List<String> entityIdentifiers = given()
            .auth()
            .preemptive()
            .basic("sysadmin", "secret")
            .when()
            .body(List.of("extraneous_key", "1_is_not_2"))
            .header("Content-type", "application/json")
            .pathParam("type", "saml20_sp")
            .put("manage/api/internal/removeExtraneousKeys/{type}")
            .as(new TypeRef<>() {
            });
        assertEquals(1, entityIdentifiers.size());
        assertEquals(metaData.getData().get("entityid"), entityIdentifiers.stream().findFirst().get());
    }

    @Test
    public void deleteIdentityProviderInPolicyUse() {
        String reasonForDeletion = "Reason for deletion";
        // IdP http://mock-idp can not be deleted as it used in policy "Regular Policy"
        Map res = given()
            .when()
            .header("Content-type", "application/json")
            .body(Collections.singletonMap("revisionNote", reasonForDeletion))
            .put("manage/api/client/metadata/saml20_idp/7")
            .as(new TypeRef<>() {
            });
        String validations = (String) res.get("validations");
        assertTrue(validations.contains("The policy Regular policy uses this IdP. First remove this IdP from the policy."));
    }

    @Test
    public void deleteServiceProviderInPolicyUse() {
        String reasonForDeletion = "Reason for deletion";
        // SP https://1234aaab.nl cannot be deleted as it used in policy "Regular Policy"
        Map res = given()
            .when()
            .auth().preemptive().basic("openconextaccess", "secret")
            .header("Content-type", "application/json")
            .body(Collections.singletonMap("revisionNote", reasonForDeletion))
            .delete("manage/api/internal/metadata/saml20_sp/11")
            .as(new TypeRef<>() {
            });
        String validations = (String) res.get("validations");
        assertTrue(validations.contains("The policy Regular policy uses this saml20_sp. First remove this saml20_sp from the policy"));
    }

    @Test
    public void internalStats() throws IOException {
        Map<String, Long> stats = given()
            .auth()
            .preemptive()
            .basic("openconextaccess", "secret")
            .when()
            .header("Content-type", "application/json")
            .get("manage/api/internal/stats")
            .as(new TypeRef<>() {
            });
        assertEquals(7L, stats.get(EntityType.SP.getType()));
        assertEquals(1L, stats.get(EntityType.RP.getType()));
        assertEquals(2L, stats.get(EntityType.IDP.getType()));
    }

    @Test
    public void deleteConsequences() {
        List<MetaData> identityProviders = given()
            .auth()
            .preemptive()
            .basic("sp-portal", "secret")
            .when()
            .header("Content-type", "application/json")
            .accept("application/json")
            .body(List.of(Map.of("type","saml20_sp","id","3"),
                Map.of("type","oidc10_rp","id","9")))
            .post("manage/api/internal/delete-consequences")
            .as(new TypeRef<>() {
            });
        assertEquals(1, identityProviders.size());
        MetaData identityProvider = identityProviders.getFirst();
        assertEquals(EntityType.IDP.getType(), identityProvider.getType());
        List<String> allowedEntityNames = ((List<Map<String, String>>) identityProvider.getData().get("allowedEntities"))
            .stream()
            .map(m -> m.get("name"))
            .toList();
        //See meta_data_seed.json SP, id 3
        assertTrue(allowedEntityNames.contains("http://mock-sp"));
    }


}
