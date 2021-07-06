package manage.service;

import manage.TestUtils;
import manage.model.MetaData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.*;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = ExporterService.class)
@TestPropertySource(locations = "classpath:test.properties")
public class ExporterServiceTest implements TestUtils {

    @Autowired
    private ExporterService subject;

    @Test
    public void exportToXml() throws Exception {
        doExportToXml(this.metaData(), "/xml/expected_metadata_export_saml20_sp.xml");
    }

    @Test
    public void exportToMapNested() throws Exception {
        MetaData metaData = this.metaData();

        Map<String, Object> result = subject.exportToMap(metaData, true);

        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        String expected = readFile("/json/expected_metadata_export_saml20_sp_nested.json");
        assertEquals(expected, json);
    }

    @Test
    public void exportToMap() throws Exception {
        MetaData metaData = this.metaData();
        Map<String, Object> result = subject.exportToMap(metaData, false);

        ExporterService.excludedDataFields.forEach(Map.class.cast(metaData.getData())::remove);

        assertEquals(result, metaData.getData());
    }

    @Test
    public void exportToXmlWithOnlyNlOrganization() throws Exception {
        MetaData metaData = this.metaData();
        Map<String, Object> metaDataFields = (Map<String, Object>) metaData.getData().get("metaDataFields");
        Arrays.asList(new String[]{"OrganizationName:en", "OrganizationURL:en", "OrganizationDisplayName:en"})
                .forEach(metaDataFields::remove);

        doExportToXml(metaData, "/xml/expected_metadata_export_saml20_sp_org_nl.xml");
    }

    @Test
    public void exportToXmlWithClassCastExceptionAttributeConsumingService() throws IOException {
        MetaData metaData = objectMapper.readValue(readFile("/json/export_attribute_consumer.json"), MetaData.class);
        String xml = subject.exportToXml(metaData);
        assertTrue(xml.contains("<md:RequestedAttribute Name=\"attribute\"/>"));
    }

    private void doExportToXml(MetaData metaData, String path) throws IOException {
        String xml = subject.exportToXml(metaData);

        assertNotNull(xml);
        String expected = readFile(path);

        //We are in a different time-zone for travis
        expected = expected.replaceFirst("validUntil=\"(.*)\"", "");
        xml = xml.replaceFirst("validUntil=\"(.*)\"", "");
        assertEquals(expected, xml);
    }


    private MetaData metaData() throws IOException {
        return objectMapper.readValue(new ClassPathResource("/json/exported_metadata_saml20_sp.json").getInputStream
                (), MetaData.class);
    }

}