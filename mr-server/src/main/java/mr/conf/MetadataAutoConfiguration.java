package mr.conf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Component
public class MetadataAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataAutoConfiguration.class);

    @Value("${metadata_configuration_path}")
    private Resource metadataConfigurationPath;

    private List<Object> schemas;


    public MetadataAutoConfiguration() throws IOException {
        this.schemas = parseConfiguration();
    }

    private List<Object> parseConfiguration() throws IOException {
        File[] schemaFiles = metadataConfigurationPath.getFile().listFiles((dir, name) -> name.endsWith("schema.json"));
        return Arrays.stream(schemaFiles).map(this::parse).collect(toList());
    }

    private Object parse(File file) {
          return null;
    }


}
