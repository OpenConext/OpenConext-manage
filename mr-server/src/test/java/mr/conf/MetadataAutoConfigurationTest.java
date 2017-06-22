package mr.conf;

import mr.validations.CertificateValidator;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;

import static org.junit.Assert.*;

public class MetadataAutoConfigurationTest {

    private MetadataAutoConfiguration subject = new MetadataAutoConfiguration(new ClassPathResource("metadata_configuration"));

    public MetadataAutoConfigurationTest() throws IOException {
    }

    @Test
    public void testSchema() throws Exception {
        String json = readFile("json/valid_service_provider.json");
        subject.validate(json, "service_provider");
    }

    private String readFile(String path) throws IOException {
        return IOUtils.toString(new ClassPathResource(path).getInputStream(), Charset.defaultCharset());
    }


}