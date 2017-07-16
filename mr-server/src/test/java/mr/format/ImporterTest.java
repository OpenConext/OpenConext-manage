package mr.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import mr.TestUtils;
import mr.conf.MetaDataAutoConfiguration;
import mr.migration.EntityType;
import org.everit.json.schema.ValidationException;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ImporterTest implements TestUtils {

    private ObjectMapper objectMapper = new ObjectMapper();

    private Importer subject = new Importer(new MetaDataAutoConfiguration(
        objectMapper,
        new ClassPathResource("metadata_configuration"),
        new ClassPathResource("metadata_templates")));


    public ImporterTest() throws IOException {
    }

    @Test
    public void importSPMetaData() throws IOException, XMLStreamException {
        String xml = readFile("/xml/metadata_import_saml20_sp.xml");
        Map<String, Object> result = subject.importXML(EntityType.SP, xml);

        assertEquals("https://teams.surfconext.nl/shibboleth", result.get("entityid"));

        Map metaDataFields = Map.class.cast(result.get(Importer.META_DATA_FIELDS));

        String json = this.readFile("/json/expected_imported_metadata_saml20_sp.json");
        assertEquals(json, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaDataFields));
    }

    @Test
    public void importIdPMetaData() throws IOException, XMLStreamException {
        String xml = readFile("/xml/metadata_import_saml20_idp.xml");
        Map<String, Object> result = subject.importXML(EntityType.IDP, xml);

        assertEquals("https://beta.surfnet.nl/simplesaml/saml2/idp/metadata.php", result.get("entityid"));

        Map metaDataFields = Map.class.cast(result.get(Importer.META_DATA_FIELDS));

        String json = this.readFile("/json/expected_imported_metadata_saml20_idp.json");
        assertEquals(json, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaDataFields));
    }

    @Test
    public void testImportSpJSONInvalid() throws IOException {
        String json = this.readFile("/json/metadata_import_saml20_sp_invalid_nested.json");
        Map map = objectMapper.readValue(json, Map.class);
        try {
            subject.importJSON(EntityType.SP, map);
        } catch (ValidationException e) {
            assertEquals(2, List.class.cast(e.toJSON().toMap().get("causingExceptions")).size());
        }
    }

    @Test
    public void testImportSpJSON() throws IOException {
        String json = this.readFile("/json/metadata_import_saml20_sp_nested.json");
        Map map = objectMapper.readValue(json, Map.class);
        Map result = subject.importJSON(EntityType.SP, map);

        assertEquals(9, result.size());

        Map metaDataFields = Map.class.cast(result.get("metaDataFields"));
        assertEquals(50, metaDataFields.size());
        assertTrue(metaDataFields.values().stream().allMatch(value -> value instanceof String));

    }

}