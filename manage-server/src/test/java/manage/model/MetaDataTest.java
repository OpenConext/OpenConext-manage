package manage.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class MetaDataTest implements TestUtils {

    private MetaData subject;

    @Before
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

        assertEquals(2, allowedEntities.size());
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