package mr.conf;

import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.Charset;

public class MetadataAutoConfigurationTest {

    private MetadataAutoConfiguration subject = new MetadataAutoConfiguration(new ClassPathResource("metadata_configuration"));

    public MetadataAutoConfigurationTest() throws IOException {
    }

    @Test
    public void testSchema() throws Exception {
        String json = readFile("json/valid_service_provider.json");
        subject.validate(json, "saml20-sp");
    }

    private String readFile(String path) throws IOException {
        return IOUtils.toString(new ClassPathResource(path).getInputStream(), Charset.defaultCharset());
    }


}