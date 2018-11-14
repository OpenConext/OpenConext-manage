package manage.format;

import manage.TestUtils;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import org.everit.json.schema.ValidationException;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("unchecked")
public class ImporterTest implements TestUtils {

    private static Importer subject;

    static {
        try {
            subject = new Importer(new MetaDataAutoConfiguration(
                    objectMapper,
                    new ClassPathResource("metadata_configuration"),
                    new ClassPathResource("metadata_templates")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void importSPMetaData() throws IOException, XMLStreamException {
        doImportSPMetaData();
    }

    private void doImportSPMetaData() throws IOException, XMLStreamException {
        String xml = readFile("/xml/metadata_import_saml20_sp.xml");
        Map<String, Object> result = subject.importXML(new ByteArrayResource(xml.getBytes()), EntityType.SP, Optional
            .empty());

        assertEquals("https://teams.surfconext.nl/shibboleth", result.get("entityid"));

        Map metaDataFields = Map.class.cast(result.get(Importer.META_DATA_FIELDS));

        String json = this.readFile("/json/expected_imported_metadata_saml20_sp.json");
        assertEquals(json, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaDataFields));
    }

    @Test
    public void importIdPMetaData() throws IOException, XMLStreamException {
        doImportIdPMetaData();
    }

    private void doImportIdPMetaData() throws IOException, XMLStreamException {
        String xml = readFile("/xml/metadata_import_saml20_idp.xml");
        Map<String, Object> result = subject.importXML(new ByteArrayResource(xml.getBytes()), EntityType.IDP,
            Optional.empty());

        assertEquals("https://beta.surfnet.nl/simplesaml/saml2/idp/metadata.php", result.get("entityid"));

        Map metaDataFields = Map.class.cast(result.get(Importer.META_DATA_FIELDS));

        String json = this.readFile("/json/expected_imported_metadata_saml20_idp.json");
        assertEquals(json, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaDataFields));
    }

    @Test
    public void importSpMetaDataWithARP() throws IOException, XMLStreamException {
        Resource resource = new GZIPClassPathResource("/xml/eduGain.xml.gz");
        Map<String, Object> result = subject.importXML(resource, EntityType.SP, Optional.of("https://wayf.nikhef" +
            ".nl/wayf/sp"));

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        String expected = readFile("json/expected_imported_metadata_edugain.json");
        assertEquals(expected, json);

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

    @Test
    public void importNoEncryptionCerts() throws IOException, XMLStreamException {
        Map<String, Object> results = this.subject.importXML(new ClassPathResource
                ("xml/FederationMetadataCertificate.xml"), EntityType.SP,
            Optional.of("http://adfs2.noorderpoort.nl/adfs/services/trust"));
        Map<String, String> metadataFields = (Map<String, String>) results.get("metaDataFields");
        assertTrue(metadataFields.containsKey("certData"));
        assertFalse(metadataFields.containsKey("certData2"));
    }

    @Test
    public void importCertificateTwice()  {
        Stream.of(EntityType.values()).forEach(entityType -> {
            try {
                Map<String, Object> data = this.subject.importXML(
                    new ClassPathResource("import_xml/adfs.mijnlentiz.nl.xml"), entityType, Optional.empty());
                Map metaDataFields = Map.class.cast(data.get("metaDataFields"));
                assertTrue(metaDataFields.containsKey("certData"));
                assertTrue(metaDataFields.containsKey("certData2"));

            } catch (Exception e) {


            }
        });
    }

    @Test
    public void aliases() throws IOException, XMLStreamException {
        Map<String, Object> metaData = this.subject.importXML(new ClassPathResource("/sp_portal/sp_xml.xml"),
            EntityType.SP, Optional.empty());
        Set<String> arpAttributes = Map.class.cast(Map.class.cast(metaData.get("arp")).get("attributes")).keySet();
        //urn:mace:dir:attribute-def:eduPersonTargetedID is alias for urn:oid:1.3.6.1.4.1.5923.1.1.1.10
        assertTrue(arpAttributes.contains("urn:mace:dir:attribute-def:eduPersonTargetedID"));
    }

    @Test
    public void testMultiplicity() throws IOException, XMLStreamException {
        MetaDataAutoConfiguration metaDataAutoConfiguration = new MetaDataAutoConfiguration(objectMapper, new ClassPathResource("metadata_configuration"), new ClassPathResource("metadata_templates"));
        Map<String, Object> spSchema = metaDataAutoConfiguration.schemaRepresentation(EntityType.SP);
        Map.class.cast(Map.class.cast(Map.class.cast(Map.class.cast(spSchema.get("properties")).get("metaDataFields")).get("patternProperties")).get("^AssertionConsumerService:([0-9]{1}):index$")).put("multiplicity",15);
        Importer alteredSubject = new Importer(metaDataAutoConfiguration);
        Map<String, Object> metaData = alteredSubject.importXML(new ClassPathResource("import_xml/assertion_consumer_service.15.xml"), EntityType.SP, Optional.empty());
        Set<Map.Entry> metaDataFields = Map.class.cast(metaData.get("metaDataFields"))
                .entrySet();
        List<String> assertionConsumerServiceList = metaDataFields.stream().filter(entry -> entry.getKey().toString().startsWith("AssertionConsumerService") && entry.getKey().toString().contains(":index")).map(entry -> entry.getValue().toString()).sorted().collect(Collectors.toList());
        assertEquals(15, assertionConsumerServiceList.size());

        String indexes = String.join(", ", assertionConsumerServiceList);
        assertEquals("111, 112, 113, 114, 115, 116, 117, 118, 127, 130, 131, 132, 250, 251, 252", indexes);

    }

}