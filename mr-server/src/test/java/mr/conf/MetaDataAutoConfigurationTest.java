package mr.conf;

import mr.migration.EntityType;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MetaDataAutoConfigurationTest {

    private MetaDataAutoConfiguration subject = new MetaDataAutoConfiguration(new ClassPathResource("metadata_configuration"));

    public MetaDataAutoConfigurationTest() throws IOException {
    }

    @Test
    public void testSchema() throws Exception {
        String json = readFile("json/valid_service_provider.json");
        subject.validate(json, EntityType.SP.getType());
    }


    @Test
    public void testSchemaInvalid() throws Exception {
        String json = readFile("json/invalid_service_provider.json");
        try {
            subject.validate(json, EntityType.SP.getType());
            fail();
        } catch (ValidationException e) {
            assertEquals(13, e.toJSON().getJSONArray("causingExceptions").toList().size());
        }

    }

    @Test
    public void testIndexConfiguration() {
        List<IndexConfiguration> indexConfigurations = subject.indexConfigurations(EntityType.SP.getType());
        assertEquals(1, indexConfigurations.size());

        IndexConfiguration indexConfiguration = indexConfigurations.get(0);
        assertEquals("text", indexConfiguration.getType());
        assertEquals("autocomplete_text_query", indexConfiguration.getName());
        assertEquals(
            asList("entityid", "metaDataFields.name:en", "metaDataFields.name:nl", "metaDataFields.description:en", "metaDataFields.description:nl"),
            indexConfiguration.getFields());

    }

    private String readFile(String path) throws IOException {
        return IOUtils.toString(new ClassPathResource(path).getInputStream(), Charset.defaultCharset());
    }


}