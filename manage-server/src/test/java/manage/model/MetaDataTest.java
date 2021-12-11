package manage.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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