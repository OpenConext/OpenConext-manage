package manage.format;

import manage.TestUtils;
import manage.model.MetaData;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;

import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ExporterTest implements TestUtils {

    private Exporter subject = new Exporter(Clock.fixed(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse
        ("2017-07-14T11:15:56.857+02:00")), ZoneId.systemDefault()),
        new DefaultResourceLoader(), "classpath:/metadata_export");

    @Test
    public void exportToXml() throws Exception {
        MetaData metaData = this.metaData();
        String xml = subject.exportToXml(metaData);

        assertNotNull(xml);
        String expected = readFile("/xml/expected_metadata_export_saml20_sp.xml");

        //We are in a different time-zone for travis
        expected = expected.replaceFirst("validUntil=\"(.*)\"", "");
        xml = xml.replaceFirst("validUntil=\"(.*)\"", "");
        assertEquals(expected, xml);
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

        Exporter.excludedDataFields.forEach(Map.class.cast(metaData.getData())::remove);

        assertEquals(result, metaData.getData());
    }

    private MetaData metaData() throws IOException {
        return objectMapper.readValue(new ClassPathResource("/json/exported_metadata_saml20_sp.json").getInputStream
            (), MetaData.class);
    }


}