package mr.format;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ImporterTest {

    private Importer subject = new Importer();

    @Test
    public void importSPMetaData() throws IOException, XMLStreamException {
        String url = new ClassPathResource("/xml/metadata_import_saml20_sp.xml").getURL().toString();
        Map<String, Object> result = subject.importURL(url);

        assertEquals("https://teams.surfconext.nl/shibboleth", result.get("entityid"));

        Map metaDataFields = Map.class.cast(result.get(Importer.META_DATA_FIELDS));

        assertNotNull(metaDataFields.get("certData"));
        System.out.println(metaDataFields);


    }

    @Test
    public void importIdPMetaData() throws IOException, XMLStreamException {
        String url = new ClassPathResource("/xml/metadata_import_saml20_idp.xml").getURL().toString();
        Map<String, Object> result = subject.importURL(url);

        assertEquals("https://beta.surfnet.nl/simplesaml/saml2/idp/metadata.php", result.get("entityid"));
    }
}