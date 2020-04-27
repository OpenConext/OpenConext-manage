package manage.model;

import manage.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
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

}