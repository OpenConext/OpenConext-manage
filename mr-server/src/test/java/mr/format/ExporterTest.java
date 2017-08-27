package mr.format;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import mr.AbstractIntegrationTest;
import mr.TestUtils;
import mr.model.MetaData;
import mr.repository.MetaDataRepository;
import org.apache.commons.io.IOUtils;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ExporterTest  implements TestUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.registerModule(new Jdk8Module());
        objectMapper.registerModule(new JavaTimeModule());
    }

    private Exporter subject = new Exporter(Clock.fixed(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse("2017-07-14T11:15:56.857+02:00")), ZoneId.systemDefault()),
        new DefaultResourceLoader(), "classpath:/metadata_export");

    @Test
    @Ignore("We are in a different time-zone for travis.")
    public void exportToXml() throws Exception {
        MetaData metaData = this.metaData();
        String xml = subject.exportToXml(metaData);

        assertNotNull(xml);
        String expected = readFile("/xml/expected_metadata_export_saml20_sp.xml");
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
        return objectMapper.readValue(new ClassPathResource("/json/exported_metadata_saml20_sp.json").getInputStream(), MetaData.class);
    }


}