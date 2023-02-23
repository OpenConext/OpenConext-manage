package manage.service;

import manage.TestUtils;
import manage.conf.MetaDataAutoConfiguration;
import manage.format.GZIPClassPathResource;
import manage.model.EntityType;
import org.everit.json.schema.ValidationException;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.mock.env.MockEnvironment;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
public class ImporterServiceTest implements TestUtils {

    private static ImporterService subject;

    static {
        try {
            subject = new ImporterService(new MetaDataAutoConfiguration(
                    objectMapper,
                    new ClassPathResource("metadata_configuration"),
                    new ClassPathResource("metadata_templates")),
                    new MockEnvironment(),
                    "nl,pt,en");
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

        Map metaDataFields = Map.class.cast(result.get(ImporterService.META_DATA_FIELDS));

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

        Map metaDataFields = Map.class.cast(result.get(ImporterService.META_DATA_FIELDS));

        String json = this.readFile("/json/expected_imported_metadata_saml20_idp.json");
        assertEquals(json, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(metaDataFields));
    }

    @Test
    public void importSpMetaDataWithARP() throws IOException, XMLStreamException {
        Resource resource = new GZIPClassPathResource("/xml/eduGain.xml.gz");
        Map<String, Object> result = subject.importXML(resource, EntityType.SP,
                Optional.of("https://wayf.nikhef.nl/wayf/sp"));
        Map expected = objectMapper.readValue(readFile("json/expected_imported_metadata_edugain.json"), Map.class);
        assertEquals(expected, result);
    }

    @Test
    public void importSpJSONInvalid() throws IOException {
        String json = this.readFile("/json/metadata_import_saml20_sp_invalid_nested.json");
        Map map = objectMapper.readValue(json, Map.class);
        try {
            subject.importJSON(EntityType.SP, map);
        } catch (ValidationException e) {
            assertEquals(2, List.class.cast(e.toJSON().toMap().get("causingExceptions")).size());
        }
    }

    @Test
    public void importSpJSON() throws IOException {
        String json = this.readFile("/json/metadata_import_saml20_sp_nested.json");
        Map map = objectMapper.readValue(json, Map.class);
        Map result = subject.importJSON(EntityType.SP, map);

        assertEquals(9, result.size());

        Map metaDataFields = Map.class.cast(result.get("metaDataFields"));
        assertEquals(50, metaDataFields.size());
        assertTrue(metaDataFields.values().stream().allMatch(value -> value instanceof String || value instanceof Boolean || value instanceof Number));

    }

    @Test
    public void importNoEncryptionCerts() throws IOException, XMLStreamException {
        Map<String, Object> results = subject.importXML(new ClassPathResource
                        ("xml/FederationMetadataCertificate.xml"), EntityType.SP,
                Optional.of("http://adfs2.noorderpoort.nl/adfs/services/trust"));
        Map<String, String> metadataFields = (Map<String, String>) results.get("metaDataFields");
        assertTrue(metadataFields.containsKey("certData"));
        assertFalse(metadataFields.containsKey("certData2"));
    }

    @Test
    public void importCertificateTwice() {
        Stream.of(EntityType.values()).forEach(entityType -> {
            try {
                Map<String, Object> data = subject.importXML(
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
        Map<String, Object> metaData = subject.importXML(new ClassPathResource("/sp_portal/sp_xml.xml"),
                EntityType.SP, Optional.empty());
        Set<String> arpAttributes = Map.class.cast(Map.class.cast(metaData.get("arp")).get("attributes")).keySet();
        //urn:mace:dir:attribute-def:eduPersonTargetedID is alias for urn:oid:1.3.6.1.4.1.5923.1.1.1.10
        assertTrue(arpAttributes.contains("urn:mace:dir:attribute-def:eduPersonTargetedID"));
    }

    @Test
    public void multiplicity() throws IOException, XMLStreamException {
        MetaDataAutoConfiguration metaDataAutoConfiguration = new MetaDataAutoConfiguration(objectMapper, new ClassPathResource("metadata_configuration"), new ClassPathResource("metadata_templates"));
        Map<String, Object> spSchema = metaDataAutoConfiguration.schemaRepresentation(EntityType.SP);
        Map.class.cast(Map.class.cast(Map.class.cast(Map.class.cast(spSchema.get("properties")).get("metaDataFields")).get("patternProperties")).get("^AssertionConsumerService:([0-3]{0,1}[0-9]{1}):index$")).put("multiplicity", 15);
        ImporterService alteredSubject = new ImporterService(metaDataAutoConfiguration, new MockEnvironment(), "nl,en,pt");
        Map<String, Object> metaData = alteredSubject.importXML(new ClassPathResource("import_xml/assertion_consumer_service.15.xml"), EntityType.SP, Optional.empty());
        Set<Map.Entry> metaDataFields = Map.class.cast(metaData.get("metaDataFields"))
                .entrySet();
        List<String> assertionConsumerServiceList = metaDataFields.stream().filter(entry -> entry.getKey().toString().startsWith("AssertionConsumerService") && entry.getKey().toString().contains(":index")).map(entry -> entry.getValue().toString()).sorted().collect(Collectors.toList());
        assertEquals(12, assertionConsumerServiceList.size());

    }

    @Test
    public void multipleCerts() throws IOException, XMLStreamException {
        Map<String, Object> metaData = subject.importXML(new ClassPathResource("/import_xml/metadata_with_attribute_authority_cert.xml"), EntityType.IDP, Optional.empty());
        assertEquals(1l, Map.class.cast(metaData.get("metaDataFields")).keySet().stream().filter(key -> String.class.cast(key).startsWith("certData")).count());
    }

    @Test
    public void ignoreNonSaml20IdpBindings() throws IOException, XMLStreamException {
        Map<String, Object> metaData = subject.importXML(new ClassPathResource("/import_xml/saml1.x_providers.xml"), EntityType.IDP, Optional.empty());
        Pattern binding = Pattern.compile("^SingleSignOnService:([0-9]{1}):Binding$");
        List<String> singleSignOnServices = ((Map<String, String>) metaData.get("metaDataFields"))
                .entrySet().stream()
                .filter(entry -> binding.matcher(entry.getKey()).matches())
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign",
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect",
                "urn:oasis:names:tc:SAML:2.0:bindings:SOAP"), singleSignOnServices);
    }

    @Test
    public void ignoreNonSaml20SPBindings() throws IOException, XMLStreamException {
        Map<String, Object> metaData = subject.importXML(new ClassPathResource("/import_xml/saml1.x_providers.xml"), EntityType.SP, Optional.empty());
        Pattern binding = Pattern.compile("^AssertionConsumerService:([0-3]{0,1}[0-9]{1}):Binding$");
        List<String> assertionConsumerServices = ((Map<String, String>) metaData.get("metaDataFields"))
                .entrySet().stream()
                .filter(entry -> binding.matcher(entry.getKey()).matches())
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST",
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST-SimpleSign",
                "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact",
                "urn:oasis:names:tc:SAML:2.0:bindings:PAOS"), assertionConsumerServices);

        List<String> singleLogoutService = ((Map<String, String>) metaData.get("metaDataFields"))
                .entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("SingleLogoutService_Binding"))
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact"), singleLogoutService);
    }

    @Test
    public void importFeedWithRegistrationInfo() throws IOException, XMLStreamException {
        List<Map<String, Object>> results = subject.importFeed(new ClassPathResource("/import_xml/edugain_mdrpi_missing.xml"))
                .stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());
        assertEquals(1, results.size());

        Map<String, Object> metaData = results.get(0);
        Map<String, String> metaDataFields = (Map<String, String>) metaData.get("metaDataFields");
        assertEquals("http://www.csc.fi/haka", metaDataFields.get("mdrpi:RegistrationInfo"));
        assertEquals("http://www.csc.fi/english/institutions/haka/instructions/join/eduGAINRegistrationStatement/", metaDataFields.get("mdrpi:RegistrationPolicy:en"));
    }

    @Test
    public void importUniqueArpValues() throws IOException, XMLStreamException {
        Map<String, Object> metaData = subject.importXML(new ClassPathResource("/import_xml/prod_md_about_spf_sps.xml"),
                EntityType.SP, Optional.of("https://sp.ota.ox.ac.uk/shibboleth"));
        List<Map<String, String>> values = (List<Map<String, String>>) ((Map) ((Map) metaData.get("arp")).get("attributes")).get("urn:mace:dir:attribute-def:cn");
        assertEquals(1, values.size());
    }
}