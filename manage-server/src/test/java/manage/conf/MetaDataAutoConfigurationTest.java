package manage.conf;

import manage.TestUtils;
import manage.exception.CustomValidationException;
import manage.model.EntityType;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.internal.URIFormatValidator;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class MetaDataAutoConfigurationTest implements TestUtils {

    private final MetaDataAutoConfiguration subject = new MetaDataAutoConfiguration(
            objectMapper,
            new ClassPathResource("metadata_configuration"),
            new ClassPathResource("metadata_templates"));

    public MetaDataAutoConfigurationTest() throws IOException {
    }

    @Test
    public void testSpDashBaord() throws IOException {
        testErrors("json/validation_error_dashboard_sp.json", EntityType.SP, 2);
    }

    @Test
    public void testSpSchema() throws IOException {
        Map<String, Object> data = objectMapper.readValue(readFile("json/valid_service_provider.json"), mapTypeRef);
        subject.validate(data, EntityType.SP.getType());
    }

    @Test
    public void testSchemaSpInvalid() throws IOException {
        testErrors("json/invalid_service_provider.json", EntityType.SP, 3);
    }

    @Test
    public void testSchemaSpForUpdateIsValid() throws IOException {
        Map<String, Object> json = objectMapper.readValue(readFile("json/updated_metadata.json"), mapTypeRef);
        subject.validate(json, EntityType.SP.getType());
    }

    @Test
    public void testIdpSchema() throws IOException {
        Map<String, Object> json = objectMapper.readValue(readFile("json/valid_identity_provider.json"), mapTypeRef);
        subject.validate(json, EntityType.IDP.getType());
    }

    @Test
    public void testSchemaIdpInvalid() throws IOException {
        testErrors("json/invalid_identity_provider.json", EntityType.IDP, 7);
    }

    @Test
    public void testAddendumServiceProvider() {
        Map metaDataFields = Map.class.cast(Map.class.cast(subject.schemaRepresentation(EntityType.SP)
                .get("properties")).get("metaDataFields"));
        Map<String, Map> properties = (Map) metaDataFields.get("properties");
        Map<String, Map> patternProperties = (Map) metaDataFields.get("patternProperties");

        assertEquals("Additional info.", properties.get("coin:additional_info").get("info"));
        assertEquals("The USP of the Service.", patternProperties.get("^usp:(en|nl)$").get("info"));
    }

    private void testErrors(String path, EntityType type, int errorsExpected) throws IOException {
        Map<String, Object> data = objectMapper.readValue(readFile(path), mapTypeRef);
        try {
            subject.validate(data, type.getType());
            fail();
        } catch (CustomValidationException e) {
            assertEquals(errorsExpected, e.getValidationException().getAllMessages().size());
        }
    }

    @Test
    public void testIndexConfiguration() {
        List<IndexConfiguration> indexConfigurations = subject.indexConfigurations(EntityType.SP.getType());
        assertEquals(0, indexConfigurations.size());

    }

    @Test
    public void testRegularExpression() {
        boolean matches = Pattern.compile("^contacts:([0-3]{1}):emailAddress$").matcher("contacts:0:emailAddress")
                .matches();
        assertTrue(matches);
    }

    @Test
    public void testUriValidator() {
        URIFormatValidator uriFormatValidator = new URIFormatValidator();
        String uri = "http://www.crossknowledge.com ";
        assertTrue(uriFormatValidator.validate(uri).isPresent());
        assertFalse(uriFormatValidator.validate(uri.trim()).isPresent());
    }

    @Test
    public void schema() {
        assertNotNull(subject.schema(EntityType.RP.getType()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void schemaNotExists() {
        subject.schema("bogus");
    }
}