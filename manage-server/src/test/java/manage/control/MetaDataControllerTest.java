package manage.control;

import com.fasterxml.jackson.core.type.TypeReference;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import manage.AbstractIntegrationTest;
import manage.model.EntityType;
import manage.model.Import;
import manage.model.MetaData;
import manage.model.MetaDataKeyDelete;
import manage.model.MetaDataUpdate;
import manage.model.Revision;
import manage.model.RevisionRestore;
import manage.oidc.OidcClient;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static io.restassured.config.RestAssuredConfig.newConfig;
import static io.restassured.config.XmlConfig.xmlConfig;
import static java.util.Collections.singletonMap;
import static manage.control.MetaDataController.ALL_ATTRIBUTES;
import static manage.control.MetaDataController.LOGICAL_OPERATOR_IS_AND;
import static manage.control.MetaDataController.REQUESTED_ATTRIBUTES;
import static manage.hook.OpenIdConnectHook.OIDC_CLIENT_KEY;
import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
                .body("size()", is(4))
                .body("title", hasItems("saml20_sp", "saml20_idp", "single_tenant_template", "oidc10_rp"));
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
    public void updateWithOIDC() {
        String body = readFile("oidc/post_path_update.json");

        given()
                .auth()
                .preemptive()
                .basic("sp-portal", "secret")
                .body(body)
                .header("Content-type", "application/json")
                .when()
                .put("/manage/api/internal/merge")
                .then()
                .statusCode(SC_OK);
    }

    @Test
    public void updateWithOIDCWithRedirectUriMap() {
        String body = readFile("oidc/post_path_update_redirect_uri_map.json");

        given()
                .auth()
                .preemptive()
                .basic("sp-portal", "secret")
                .body(body)
                .header("Content-type", "application/json")
                .when()
                .put("/manage/api/internal/merge")
                .then()
                .statusCode(SC_INTERNAL_SERVER_ERROR);
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
    public void autoAlternatives() {
        given()
                .when()
                .queryParam("query", "OIDC Resource")
                .get("manage/api/client/autocomplete/saml20_sp")
                .then()
                .statusCode(SC_OK)
                .body("suggestions.size()", is(0))
                .body("alternatives.data.entityid", hasItems(
                        "https@//oidc.rp.resourceServer"));
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
                .body("size()", is(1))
                .body("[0].data.metaDataFields.'scopes'", hasItems("openid", "groups"));
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
        doUpdate(EntityType.SP, "1", "First revision SP");
        doUpdate(EntityType.RP, "9", "First revision RP");
        doUpdate(EntityType.IDP, "6", "First revision IDP");
        doUpdate(EntityType.SP, "1", "Second revision SP");
        doUpdate(EntityType.SP, "1", "Third revision SP");

        Map<String, Object> body = new HashMap<>();
        body.put("types", Arrays.asList(EntityType.RP.getType(), EntityType.IDP.getType(), EntityType.SP.getType()));
        body.put("limit", 4);
        List<Map<String, Object>> results = given()
                .when()
                .header("Content-type", "application/json")
                .body(body)
                .post("manage/api/client/recent-activity")
                .as(mapListTypeRef);

        assertEquals(4, results.size());

        Map<String, Object> sp1 = results.get(0);
        assertEquals("1", sp1.get("id"));
        assertEquals("Third revision SP", ((Map)sp1.get("data")).get("revisionnote"));

        Map<String, Object> idp6 = results.get(1);
        assertEquals("6", idp6.get("id"));
        assertEquals("First revision IDP", ((Map)idp6.get("data")).get("revisionnote"));

        Map<String, Object> rp9 = results.get(2);
        assertEquals("9", rp9.get("id"));
        assertEquals("First revision RP", ((Map)rp9.get("data")).get("revisionnote"));
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
                .body("size()", is(3))
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
    public void createOidc() throws java.io.IOException {
        String json = readFile("/json/valid_service_provider.json");
        Map data = objectMapper.readValue(json, Map.class);

        OidcClient oidcClient = new OidcClient("http://client_id", "secret", Collections.singleton("http://redirect"), "authorization_code", Collections.singleton("openid"));
        data.put(OIDC_CLIENT_KEY, oidcClient);

        MetaData metaData = new MetaData(EntityType.SP.getType(), data);
        metaData.metaDataFields().remove("AssertionConsumerService:0:Binding");
        metaData.metaDataFields().remove("AssertionConsumerService:0:Location");
        metaData.metaDataFields().put("coin:oidc_client", "1");
        metaData.getData().put("entityid", "http://oidc_client");

        Map map = given().auth()
                .preemptive()
                .basic("sp-portal", "secret")
                .when()
                .body(metaData)
                .header("Content-type", "application/json")
                .post("manage/api/internal/metadata")
                .getBody().as(Map.class);

        Map oidcClientMap = Map.class.cast(Map.class.cast(map.get("data")).get(OIDC_CLIENT_KEY));
        assertEquals("http@//oidc_client", oidcClientMap.get("clientId"));

        Map results = given()
                .when()
                .get("manage/api/client/metadata/saml20_sp/" + map.get("id"))
                .getBody().as(Map.class);

        oidcClientMap = Map.class.cast(Map.class.cast(results.get("data")).get(OIDC_CLIENT_KEY));
        assertEquals("http@//oidc_client", oidcClientMap.get("clientId"));
    }

    @Test
    public void updateOicdClient() {
        MetaData metaData = given()
                .when()
                .get("manage/api/client/metadata/saml20_sp/8")
                .getBody().as(MetaData.class);

        Map<String, Object> pathUpdates = new HashMap<>();
        pathUpdates.put("metaDataFields.name:en", "New name");

        Map<String, Object> externalReferenceData = new HashMap<>();
        Map<String, Object> oidcClient = (Map<String, Object>) metaData.getData().get(OIDC_CLIENT_KEY);
        List<String> rediredctUris = Collections.singletonList("http://new-redirect");
        oidcClient.put("redirectUris", rediredctUris);
        externalReferenceData.put(OIDC_CLIENT_KEY, oidcClient);

        MetaDataUpdate metaDataUpdate = new MetaDataUpdate("8", EntityType.SP.getType(), pathUpdates, externalReferenceData);

        MetaData result = given()
                .auth()
                .preemptive()
                .basic("sp-portal", "secret")
                .body(metaDataUpdate)
                .header("Content-type", "application/json")
                .when()
                .put("/manage/api/internal/merge")
                .getBody()
                .as(MetaData.class);
        oidcClient = (Map<String, Object>) result.getData().get(OIDC_CLIENT_KEY);
        assertEquals(rediredctUris, oidcClient.get("redirectUris"));
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
        assertTrue(((String) data.get("revisionnote")).startsWith("Connection created by Dashboard on request of John Doe"));

        List<Map> allowedEntities = (List<Map>) data.get("allowedEntities");

        assert listOfMapsContainsValue(allowedEntities, spId);
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
        assertEquals(data.get("revisionnote"), "Connection created by Dashboard on request of John Doe - urn:collab:person:example.com:jdoe");

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
    public void oidcMerge() {
        List<Map<String, Object>> results = given().auth()
                .preemptive()
                .basic("sp-portal", "secret")
                .when()
                .body(Collections.singletonList("https://oidc.test.client"))
                .header("Content-type", "application/json")
                .put("manage/api/internal/oidc/merge")
                .as(List.class);

        assertEquals(1, results.size());

        Map<String, Object> metaData = results.get(0);
        Map<String, Object> data = (Map<String, Object>) metaData.get("data");
        assertEquals("oidc.test.client", data.get("entityid"));
        assertEquals("Connection created by OIDC Merge for https://oidc.test.client on request of sp-portal", data.get("revisionnote"));

        Map<String, Object> metaDataFields = (Map<String, Object>) data.get("metaDataFields");
        assertEquals("authorization_code", ((List) metaDataFields.get("grants")).get(0));
        assertEquals("openid", ((List) metaDataFields.get("scopes")).get(0));

        validateMergedOidc(results);

        MetaData idp = metaDataRepository.findById("6", EntityType.IDP.getType());
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) idp.getData().get("allowedEntities");
        long count = allowedEntities.stream().filter(map -> map.get("name").equals("oidc.test.client")).count();
        assertEquals(1L, count);
    }

    @Test
    public void oidcMergeValidationErrors() throws IOException {
        MetaData metaData = objectMapper.readValue(readFile("json/oidc_merge_json_export_manage.json"), new
                TypeReference<MetaData>() {
                });

        metaDataRepository.save(metaData);

        List<Map<String, Object>> results = given().auth()
                .preemptive()
                .basic("sp-portal", "secret")
                .when()
                .body(Collections.singletonList("https://ahk.leerpodium.nl"))
                .header("Content-type", "application/json")
                .put("manage/api/internal/oidc/merge")
                .as(List.class);

        assertEquals(1, results.size());

        validateMergedOidc(results);
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

    private void validateMergedOidc(List<Map<String, Object>> results) {
        Map<String, Object> metaData = results.get(0);
        Map oidcRp = given()
                .when()
                .get("manage/api/client/metadata/oidc10_rp/" + metaData.get("id"))
                .as(Map.class);

        //The secret is hashed
        ((Map) ((Map) oidcRp.get("data")).get("metaDataFields")).remove("secret");
        Map<String, Object> data = (Map<String, Object>) metaData.get("data");

        Map<String, Object> metaDataFields = (Map<String, Object>) data.get("metaDataFields");
        metaDataFields.remove("secret");

        assertEquals(metaData, oidcRp);
    }
}