package manage.conf;

import manage.TestUtils;
import manage.migration.EntityType;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.internal.URIFormatValidator;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@SuppressWarnings("unchecked")
public class MetaDataAutoConfigurationTest implements TestUtils {

    private MetaDataAutoConfiguration subject = new MetaDataAutoConfiguration(
        objectMapper,
        new ClassPathResource("metadata_configuration"),
        new ClassPathResource("metadata_templates"));

    public MetaDataAutoConfigurationTest() throws IOException {
    }

    @Test
    public void testSpDashBaord() throws Exception {
        testErrors("json/validation_error_dashboard_sp.json", EntityType.SP, 2);
    }

    @Test
    public void testSpSchema() throws Exception {
        String json = readFile("json/valid_service_provider.json");
        subject.validate(json, EntityType.SP.getType());
    }

    @Test
    public void testSchemaSpInvalid() throws Exception {
        testErrors("json/invalid_service_provider.json", EntityType.SP, 3);
    }

    @Test
    public void testSchemaSpForUpdateIsValid() throws Exception {
        String json = readFile("json/updated_metadata.json");
        subject.validate(json, EntityType.SP.getType());
    }

    @Test
    public void testIdpSchema() throws Exception {
        String json = readFile("json/valid_identity_provider.json");
        subject.validate(json, EntityType.IDP.getType());
    }

    @Test
    public void testSchemaIdpInvalid() throws Exception {
        testErrors("json/invalid_identity_provider.json", EntityType.IDP, 13);
    }

    private void testErrors(String path, EntityType type, int errorsExpected) {
        String json = readFile(path);
        try {
            subject.validate(json, type.getType());
            fail();
        } catch (ValidationException e) {
            assertEquals(errorsExpected, e.getAllMessages().size());
        }
    }

    @Test
    public void testMetaDataMotivations() {
        Map metaDataFields = (Map) Map.class.cast(subject.schemaRepresentation(EntityType.SP).get("properties")).get
            ("metaDataFields");
        Map<String, String> keys = (Map<String, String>) Map.class.cast(metaDataFields.get("properties")).keySet()
            .stream()
            .filter(key -> String.class.cast(key).startsWith("coin:attr_motivation:")).collect(Collectors.toMap(k -> {
                String key = (String) k;
                return key.substring(key.lastIndexOf(":") + 1);
            }, k -> k));
        System.out.println(String.join(", ",  keys.values().stream().map(s -> (String) "\""+s +"\"").collect(Collectors.toList())));

        Map<String, String> arpAttributes = (Map<String, String>) Map.class.cast(Map.class.cast(Map.class.cast(Map
            .class.cast(Map.class.cast(subject.schemaRepresentation(EntityType.SP).get("properties")).get("arp")).get
            ("properties")).get("attributes")).get("properties"))
            .keySet().stream().collect(Collectors.toMap(k -> {
                String key = (String) k;
                return key.substring(key.lastIndexOf(":") + 1);
            }, k -> k));
        System.out.println(arpAttributes);
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

}