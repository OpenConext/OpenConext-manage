package mr.conf;

import mr.validations.CertificateValidator;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.security.cert.CertificateException;

import static org.junit.Assert.*;

public class MetadataAutoConfigurationTest {

    private MetadataAutoConfiguration subject = new MetadataAutoConfiguration(new ClassPathResource("metadata_configuration"));

    public MetadataAutoConfigurationTest() throws CertificateException, IOException {
    }

    @Test
    public void testSchema() throws Exception {
        Schema schema = subject.schema("service_provider");
        schema.validate(jsonObject("json/valid_service_provider.json"));
    }

    private JSONObject jsonObject(String path) throws IOException {
        return new JSONObject(new JSONTokener(new ClassPathResource(path).getInputStream()));
    }

}