package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.restassured.common.mapper.TypeRef;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import manage.AbstractIntegrationTest;
import manage.model.*;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.query.Query;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static io.restassured.config.XmlConfig.xmlConfig;
import static java.util.Collections.singletonMap;
import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;
import static manage.service.MetaDataService.*;
import static org.apache.http.HttpStatus.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

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
    public void remove() {
        MetaData metaData = metaDataRepository.findById("1", "saml20_sp");
        assertNotNull(metaData);

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

        List<MetaData> revisions = metaDataRepository.findRaw("saml20_sp_revision", "{\"revision.parentId\": \"1\"}");
        assertEquals(2, revisions.size());

        MetaData revision = revisions.get(1);
        assertEquals(revision.getData().get("revisionnote"), reasonForDeletion);
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
                .body("size()", is(5))
                .body("title", hasItems("saml20_sp", "saml20_idp", "single_tenant_template", "oidc10_rp", "oauth20_rs"));
    }

    @Test
    public void post() throws IOException {
        doPost(true, "saml2_user.com");
    }

    @Test
    public void postInternal() throws IOException {
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

    /**
     * This is an outstanding bug where Manage cast numeric strings to int. Won't be fixed to
     * maintain backward compatibility for scrips that assume all metadata values are strings.
     */
    @Test
    public void searchBug() {
        Map<String, Object> searchOptions = new HashMap<>();
        searchOptions.put("metaDataFields.coin:institution_id", "123");

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

        Map<String, Object> rs10 = results.get(3);
        assertEquals("Delete revision RS 10", ((Map) rs10.get("data")).get("revisionnote"));
        assertNotNull(((Map) rs10.get("revision")).get("terminated"));

        Map<String, Object> sp2 = results.get(4);
        assertEquals("Delete revision SP 2", ((Map) sp2.get("data")).get("revisionnote"));
        assertNotNull(((Map) sp2.get("revision")).get("terminated"));
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
                .body(emptyOrNullString());
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

        doNewSp(body)
                .statusCode(SC_BAD_REQUEST)
                .body("message", equalTo(String.format("There already exists a MetaData entry with entityId: %s",
                        entityId)));
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
        assertEquals(updated.getData().get("entityid"),
                "https://engine.test2.surfconext.nl/authentication/sp/metadata");
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
        revisions.forEach(rev -> assertEquals(rev.getRevision().getParentId(), "1"));
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

        importRequest = new Import(urlS, "https://impacter.eu/sso/metadata");
        result = given()
                .body(importRequest)
                .header("Content-type", "application/json")
                .post("manage/api/client/import/endpoint/xml/saml20_sp")
                .getBody()
                .as(Map.class);
        Set<String> keysFromMetaData = metaData.metaDataFields().keySet();
        Set<String> metaDataFields = ((Map) result.get("metaDataFields")).keySet();

        keysFromMetaData.removeAll(metaDataFields);
        assertEquals(Arrays.asList("coin:imported_from_edugain", "coin:interfed_source"), new ArrayList(keysFromMetaData));
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
        revisions.forEach(rev -> assertEquals(rev.getRevision().getParentId(), "1"));

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
        String json = readFile("/metadata_templates/oidc10_rp.template.json");

        Map data = objectMapper.readValue(json, Map.class);
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
            assertEquals(false, metaData.metaDataFields().containsKey(keyToDelete));
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

        given()
                .auth()
                .preemptive()
                .basic("sp-portal", "secret")
                .header("Content-type", "application/json")
                .body(connectionData)
                .put("manage/api/internal/connectWithoutInteraction/")
                .then()
                .statusCode(SC_OK);

        MetaData idp = metaDataRepository.findById(idUId, EntityType.IDP.getType());
        Map<String, Object> data = idp.getData();
        List<Map> allowedEntities = (List<Map>) data.get("allowedEntities");

        assertTrue(listOfMapsContainsValue(allowedEntities, spId));
    }

    @Test
    public void connectSpWithSameInstitutionIdAsIdp() {
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
                .basic("sp-portal", "secret")
                .header("Content-type", "application/json")
                .body(connectionData)
                .put("manage/api/internal/connectWithoutInteraction/")
                .then()
                .statusCode(SC_OK);

        MetaData idp = metaDataRepository.findById(idUId, EntityType.IDP.getType());
        Map<String, Object> data = idp.getData();
        assertEquals(data.get("revisionnote"), "Connected https@//oidc.rp on request of John Doe - urn:collab:person:example.com:jdoe via Dashboard.");

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
                .put("manage/api/internal/connectWithoutInteraction/")
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
                .basic("sp-portal", "secret")
                .header("Content-type", "application/json")
                .body(connectionData)
                .put("manage/api/internal/connectWithoutInteraction/")
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
                .basic("sp-portal", "secret")
                .header("Content-type", "application/json")
                .body(connectionData)
                .put("manage/api/internal/connectWithoutInteraction/")
                .then()
                .statusCode(SC_NOT_FOUND);
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
        String json = readFile("/json/sp_dashboard_arp_array.json");
        Map data = objectMapper.readValue(json, Map.class);
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
    public void acceptChangeRequest() {
        doCreateChangeRequest();
        MetaDataChangeRequest metaDataChangeRequest = mongoTemplate()
                .find(new Query(), MetaDataChangeRequest.class, EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX)).get(0);
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(new ChangeRequest(metaDataChangeRequest.getId(), EntityType.SP.getType(), metaDataChangeRequest.getMetaDataId()))
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
    public void rejectChangeRequest() {
        doCreateChangeRequest();
        MetaDataChangeRequest metaDataChangeRequest = mongoTemplate()
                .find(new Query(), MetaDataChangeRequest.class, EntityType.SP.getType().concat(CHANGE_REQUEST_POSTFIX)).get(0);
        given()
                .when()
                .contentType(ContentType.JSON)
                .body(new ChangeRequest(metaDataChangeRequest.getId(), EntityType.SP.getType(), metaDataChangeRequest.getMetaDataId()))
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

    private void doCreateChangeRequest() {
        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.description:en", "New description");
        pathUpdates.put("allowedall", false);
        pathUpdates.put("allowedEntities", Arrays.asList(singletonMap("name", "https://allow-me"),
                singletonMap("name", "http://mock-idp")));
        Map<String, Object> auditData = new HashMap<>();
        auditData.put("user", "jdoe");

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest(
                "1", EntityType.SP.getType(), pathUpdates, auditData
        );
        given().auth().preemptive().basic("sp-portal", "secret")
                .when()
                .body(changeRequest)
                .header("Content-type", "application/json")
                .post("manage/api/internal/change-requests")
                .then()
                .statusCode(SC_OK);
    }
}