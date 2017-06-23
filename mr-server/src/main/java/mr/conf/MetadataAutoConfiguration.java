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
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
public class MetadataAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MetadataAutoConfiguration.class);

    private Map<String, Schema> schemas;
    private Map<String, List<IndexConfiguration>> indexConfigurations = new HashMap<>();

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
        Schema schema = schemas.computeIfAbsent(type, key -> {
            throw new IllegalArgumentException(String.format("No schema defined for %s", key));
        });
        schema.validate(jsonObject);
    }

    public Set<String> schemaNames() {
        return schemas.keySet();
    }

    public List<IndexConfiguration> indexConfigurations(String schemaType) {
        return this.indexConfigurations.getOrDefault(schemaType, Collections.emptyList());
    }

    private Map<String, Schema> parseConfiguration(Resource metadataConfigurationPath, List<FormatValidator> validators) throws IOException {
        File[] schemaFiles = metadataConfigurationPath.getFile().listFiles((dir, name) -> name.endsWith("schema.json"));
        Assert.notEmpty(schemaFiles, String.format("No schema.json files defined in %s", metadataConfigurationPath.getFilename()));
        return Arrays.stream(schemaFiles).map(file -> this.parse(file, validators))
            .collect(toMap(Schema::getTitle, schema -> schema));
    }

    private Schema parse(File file, List<FormatValidator> validators) {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(new JSONTokener(new FileInputStream(file)));
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(String.format("%s not found", file.getAbsolutePath()));
        }
        SchemaLoader.SchemaLoaderBuilder schemaLoaderBuilder = SchemaLoader.builder().schemaJson(jsonObject);
        validators.forEach(schemaLoaderBuilder::addFormatValidator);
        Schema schema = schemaLoaderBuilder.build().load().build();
        addIndexes(schema.getTitle(), jsonObject);
        return schema;
    }

    @SuppressWarnings("unchecked")
    private void addIndexes(String schemaType, JSONObject json) {
        if (json.has("indexes")) {
            List<Object> indexes = json.getJSONArray("indexes").toList();
            List<IndexConfiguration> indexConfigurations = indexes.stream().map(obj -> {
                Map map = Map.class.cast(obj);
                return new IndexConfiguration((String) map.get("name"), (String) map.get("type"), (List<String>) map.get("fields"));
            }).collect(toList());
            this.indexConfigurations.put(schemaType, indexConfigurations);
        }

    }

}
