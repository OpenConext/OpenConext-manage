package mr.conf;

import mr.validations.BooleanValidator;
import mr.validations.CertificateValidator;
import org.everit.json.schema.FormatValidator;
import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toMap;

@Component
public class MetadataAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataAutoConfiguration.class);

    private Map<String, Schema> schemas;

    @Autowired
    public MetadataAutoConfiguration(@Value("${metadata_configuration_path}") Resource metadataConfigurationPath) throws IOException {
        this.schemas = parseConfiguration(metadataConfigurationPath, Arrays.asList(
            new CertificateValidator(),
            new BooleanValidator()
        ));
        LOG.info("Finished loading {} metadata configurations", schemas.size());
    }


    public void validate(String json, String type) {
        JSONObject jsonObject = new JSONObject(new JSONTokener(json));
        Schema schema = schema(type);
        schema.validate(jsonObject);
    }

    public Set<String> schemaNames() {
        return schemas.keySet();
    }

    private Schema schema(String type) {
        return schemas.computeIfAbsent(type, key -> {
            throw new IllegalArgumentException(String.format("No schema defined for {}", key));
        });
    }

    private Map<String, Schema> parseConfiguration(Resource metadataConfigurationPath, List<FormatValidator> validators) throws IOException {
        File[] schemaFiles = metadataConfigurationPath.getFile().listFiles((dir, name) -> name.endsWith("schema.json"));
        return Arrays.stream(schemaFiles).map(file -> this.parse(file, validators))
            .collect(toMap(schema -> schema.getTitle(), schema -> schema));
    }

    private Schema parse(File file, List<FormatValidator> validators) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(new JSONTokener(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(String.format("%s not found", file.getAbsolutePath()));
        }
        SchemaLoader.SchemaLoaderBuilder schemaLoaderBuilder = SchemaLoader.builder().schemaJson(jsonObject);
        validators.forEach(validator -> schemaLoaderBuilder.addFormatValidator(validator));
        return schemaLoaderBuilder.build().load().build();
    }

}
