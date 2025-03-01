package manage.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class MetaDataTest implements TestUtils {

    private MetaData subject;

    @BeforeEach
    public void before() throws IOException {
        subject = objectMapper.readValue(readFile("json/meta_data_detail.json"), MetaData.class);
    }

    @Test
    public void revision() throws Exception {
        subject.revision("new_id");
        assertEquals("new_id", subject.getId());
    }

    @Test
    public void promoteToLatest() throws Exception {
        int pre = subject.getRevision().getNumber();
        subject.promoteToLatest("test", "revision-notes");

        assertEquals("revision-notes", subject.getData().get("revisionnote"));
        assertEquals(pre + 1, subject.getRevision().getNumber());
    }

    @Test
    public void merge() {
        Map<String, Object> pathUpdates = Collections.singletonMap("metaDataFields.name:en", "Changed");
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate("id", "saml20_sp", pathUpdates, Collections.emptyMap());
        subject.merge(metaDataUpdate);

        assertEquals("Changed", subject.metaDataFields().get("name:en"));
    }

    @Test
    public void mergeIncrementalAddition() {
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) subject.getData().get("allowedEntities");
        allowedEntities.add(Map.of("name", "existing_entity"));

        Map<String, Object> pathUpdates = Map.of("allowedEntities", Map.of("name", "new_entity"));
        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.ADDITION);

        subject.merge(changeRequest);

        allowedEntities = (List<Map<String, String>>) subject.getData().get("allowedEntities");
        assertEquals(2, allowedEntities.size());
    }

    @Test
    public void mergeIncrementalMultipleAddition() {
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) subject.getData().get("allowedEntities");
        allowedEntities.add(Map.of("name", "existing_entity"));

        Map<String, Object> pathUpdates = Map.of("allowedEntities",
                List.of(Map.of("name", "new_entity"), Map.of("name", "new_entity2")));

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.ADDITION);

        subject.merge(changeRequest);

        allowedEntities = (List<Map<String, String>>) subject.getData().get("allowedEntities");
        assertEquals(3, allowedEntities.size());
    }

    @Test
    public void mergeIncrementalMultipleRemoval() {
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) subject.getData().get("allowedEntities");
        allowedEntities.add(Map.of("name", "entity_1"));
        allowedEntities.add(Map.of("name", "entity_2"));

        Map<String, Object> pathUpdates = Map.of("allowedEntities",
                List.of(Map.of("name", "entity_1"), Map.of("name", "entity_2")));

        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.REMOVAL);

        subject.merge(changeRequest);

        assertEquals(0, allowedEntities.size());
    }

    @Test
    public void mergeIncrementalAdditionNewAttribute() {
        subject.getData().remove("allowedEntities");

        Map<String, Object> pathUpdates = Map.of("allowedEntities", Map.of("name", "new_entity"));
        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.ADDITION);

        subject.merge(changeRequest);

        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) subject.getData().get("allowedEntities");
        assertEquals(1, allowedEntities.size());
    }

    @Test
    public void mergeIncrementalRemoval() {
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) subject.getData().get("allowedEntities");
        allowedEntities.add(Map.of("name", "existing_entity"));
        allowedEntities.add(Map.of("name", "to_be_removed_entity"));

        Map<String, Object> pathUpdates = Map.of("allowedEntities", Map.of("name", "to_be_removed_entity"));
        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.REMOVAL);

        subject.merge(changeRequest);

        assertEquals(1, allowedEntities.size());
        assertEquals("existing_entity", allowedEntities.get(0).get("name"));
    }

    @Test
    public void mergeIncrementalRemovalNullValue() {
        subject.getData().remove("allowedEntities");

        Map<String, Object> pathUpdates = Map.of("allowedEntities", Map.of("name", "to_be_removed_entity"));
        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.REMOVAL);

        subject.merge(changeRequest);
        assertFalse(subject.getData().containsKey("allowedEntities"));
    }

    @Test
    public void mergeIncrementalAdditionUpdate() {
        subject.getData().put("stepupEntities",
                List.of(Map.of("name", "entity_id", "level", "loa2")));

        Map<String, Object> pathUpdates = Map.of("stepupEntities", Map.of("name", "entity_id", "level", "loa3"));
        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.ADDITION);

        subject.merge(changeRequest);

        List<Map<String, String>> stepupEntities = (List<Map<String, String>>) subject.getData().get("stepupEntities");
        assertEquals(1, stepupEntities.size());
        assertEquals("loa3", stepupEntities.get(0).get("level"));
    }

    @Test
    public void mergeIncrementalAdditionUpdateMultiple() {
        subject.getData().put("stepupEntities",
                List.of(Map.of("name", "entity_id", "level", "loa2"),
                        Map.of("name", "sp_id", "level", "loa2")));

        Map<String, Object> pathUpdates = Map.of("stepupEntities",
                List.of(Map.of("name", "entity_id", "level", "loa3"),
                        Map.of("name", "sp_id", "level", "loa3")));
        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.ADDITION);

        subject.merge(changeRequest);

        List<Map<String, String>> stepupEntities = (List<Map<String, String>>) subject.getData().get("stepupEntities");
        assertEquals(2, stepupEntities.size());
        stepupEntities.forEach(entity -> {
            assertEquals("loa3", entity.get("level"));
        });

    }

    @Test
    public void mergeIncrementalAdditionArp() {
        List<Map<String, String>> orcidArpValue = List.of(Map.of("value", "*", "source", "idp"));
        Map<String, Object> pathUpdates = Map.of("arp.attributes",
                Map.of("urn:mace:dir:attribute-def:eduPersonOrcid", orcidArpValue));
        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.ADDITION);

        Map<String, Object> attributes = (Map<String, Object>) ((Map) subject.getData().get("arp")).get("attributes");
        assertEquals(5, attributes.size());

        subject.merge(changeRequest);

        Map<String, Object> newAttributes = (Map<String, Object>) ((Map) subject.getData().get("arp")).get("attributes");
        assertEquals(orcidArpValue, newAttributes.get("urn:mace:dir:attribute-def:eduPersonOrcid"));
        assertEquals(6, newAttributes.size());
    }

    @Test
    public void mergeIncrementalRemovalArp() {
        List<Map<String, String>> mailArpValue = List.of(Map.of("value", "*", "source", "idp"));
        Map<String, Object> pathUpdates = Map.of("arp.attributes",
                Map.of("urn:mace:dir:attribute-def:mail", mailArpValue));
        MetaDataChangeRequest changeRequest = new MetaDataChangeRequest("id", "saml20_sp", "note", pathUpdates, Collections.emptyMap());
        changeRequest.setIncrementalChange(true);
        changeRequest.setPathUpdateType(PathUpdateType.REMOVAL);

        Map<String, Object> attributes = (Map<String, Object>) ((Map) subject.getData().get("arp")).get("attributes");
        assertEquals(5, attributes.size());

        subject.merge(changeRequest);

        Map<String, Object> newAttributes = (Map<String, Object>) ((Map) subject.getData().get("arp")).get("attributes");
        assertFalse(newAttributes.containsKey("urn:mace:dir:attribute-def:mail"));
        assertEquals(4, newAttributes.size());
    }

    @Test
    public void equals() {
        assertTrue(subject.equals(subject));
    }

    @Test
    public void trimSpaces() throws JsonProcessingException {
        MetaData metaData = objectMapper.readValue(readFile("json/meta_data_with_spacesl.json"), MetaData.class);

        assertEquals(" SURFnet BV ", metaData.metaDataFields().get("OrganizationName:nl"));
        Map displayNameArp = (Map) ((List) ((Map) ((Map) metaData.getData().get("arp")).get("attributes")).get("urn:mace:dir:attribute-def:displayName")).get(0);
        assertEquals(" * ", displayNameArp.get("value"));
        assertEquals(" idp ", displayNameArp.get("source"));

        metaData.trimSpaces();

        assertEquals("SURFnet BV", metaData.metaDataFields().get("OrganizationName:nl"));
        displayNameArp = (Map) ((List) ((Map) ((Map) metaData.getData().get("arp")).get("attributes")).get("urn:mace:dir:attribute-def:displayName")).get(0);
        assertEquals("*", displayNameArp.get("value"));
        assertEquals("idp", displayNameArp.get("source"));

    }
}