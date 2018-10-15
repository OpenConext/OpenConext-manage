package manage.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import manage.model.EntityType;
import manage.validations.BooleanFormatValidator;
import manage.validations.CertificateFormatValidator;
import manage.validations.LocalEmailFormatValidator;
import manage.validations.NumberFormatValidator;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Component
public class MetaDataAutoConfiguration {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataAutoConfiguration.class);

    private Map<String, Schema> schemas;
    private Map<String, File> templates;
    private List<Map<String, Object>> schemaRepresentations = new ArrayList<>();
    private Map<String, List<IndexConfiguration>> indexConfigurations = new HashMap<>();
    private ObjectMapper objectMapper;

    @Autowired
    public MetaDataAutoConfiguration(ObjectMapper objectMapper,
                                     @Value("${metadata_configuration_path}") Resource metadataConfigurationPath,
                                     @Value("${metadata_templates_path}") Resource metadataTemplatesPath) throws
        IOException {
        this.schemas = parseConfiguration(metadataConfigurationPath, Arrays.asList(
            new CertificateFormatValidator(),
            new NumberFormatValidator(),
            new BooleanFormatValidator(),
            new LocalEmailFormatValidator()
        ));
        this.objectMapper = objectMapper;
        this.templates = parseTemplates(metadataTemplatesPath);
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

    public Map<String, Object> metaDataTemplate(String type) {
        File file = templates.computeIfAbsent(type, key -> {
            throw new IllegalArgumentException(String.format("No template defined for %s", key));
        });
        return this.parseTemplate(file);
    }

    public List<IndexConfiguration> indexConfigurations(String schemaType) {
        return this.indexConfigurations.getOrDefault(schemaType, Collections.emptyList());
    }

    public List<Map<String, Object>> schemaRepresentations() {
        return schemaRepresentations;
    }

    public Map<String, Object> schemaRepresentation(EntityType entityType) {
        Optional<Map<String, Object>> schemaRepresentationOptional = schemaRepresentations().stream().filter(map ->
            map.get("title").equals(entityType.getType())).findFirst();
        return schemaRepresentationOptional.orElseThrow(() -> new IllegalArgumentException(String.format("The %s " +
            "schema does not exists", entityType.getType())));

    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    private Map<String, Schema> parseConfiguration(Resource metadataConfigurationPath, List<FormatValidator>
        validators) throws IOException {
        File[] files = metadataConfigurationPath.getFile().listFiles();

        List<File> schemaFiles =
            Stream.of(files).filter(file -> file.getName().endsWith("schema.json")).collect(Collectors.toList());
        Assert.notEmpty(schemaFiles, String.format("No schema.json files defined in %s", metadataConfigurationPath
            .getFilename()));
        return schemaFiles.stream().map(file -> this.parse(file, validators,
            Stream.of(files).filter(f -> f.getName().equals(file.getName().replace("schema", "addendum"))).findAny()))
            .collect(toMap(Schema::getTitle, schema -> schema));
    }


    private Map<String, File> parseTemplates(Resource metadataTemplatesPath) throws IOException {
        File[] templates = metadataTemplatesPath.getFile().listFiles((dir, name) -> name.endsWith("template.json"));
        Assert.notEmpty(templates, String.format("No template.json files defined in %s", metadataTemplatesPath
            .getFilename()));
        return Stream.of(templates).collect(toMap(file -> file.getName().substring(0, file.getName().indexOf(
            ".template.json")), file -> file));
    }

    private Schema parse(File file, List<FormatValidator> validators, Optional<File> addendum) {
        JSONObject jsonObject = new JSONObject(new JSONTokener(this.fileInputStream(file)));
        Optional<JSONObject> optionalAddendum =
            addendum.map(addendumFile -> new JSONObject(new JSONTokener(this.fileInputStream(addendumFile))));
        if (optionalAddendum.isPresent()) {
            jsonObject = this.deepMerge(jsonObject, optionalAddendum.get());
        }
        SchemaLoader.SchemaLoaderBuilder schemaLoaderBuilder = SchemaLoader.builder().schemaJson(jsonObject);
        validators.forEach(schemaLoaderBuilder::addFormatValidator);
        Schema schema = schemaLoaderBuilder.build().load().build();
        addIndexes(schema.getTitle(), jsonObject);
        this.schemaRepresentations.add(jsonObject.toMap());
        return schema;
    }

    private JSONObject deepMerge(JSONObject source, JSONObject addendum) {
        Stream.of(JSONObject.getNames(addendum)).forEach(name -> {
                Object value = addendum.get(name);
                if (!source.has(name)) {
                    source.put(name, value);
                } else {
                    if (value instanceof JSONObject) {
                        JSONObject addendumValueJson = (JSONObject) value;
                        JSONObject jsonObject = source.getJSONObject(name);
                        deepMerge(jsonObject, addendumValueJson);
                    } else {
                        source.put(name, value);
                    }
                }
            }
        );
        return source;
    }

    private FileInputStream fileInputStream(File file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new IllegalArgumentException(String.format("%s not found", file.getAbsolutePath()));
        }
    }

    private Map<String, Object> parseTemplate(File file) {
        try {
            return objectMapper.readValue(file, Map.class);
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void addIndexes(String schemaType, JSONObject json) {
        if (json.has("indexes")) {
            List<Object> indexes = json.getJSONArray("indexes").toList();
            List<IndexConfiguration> indexConfigurations = indexes.stream().map(obj -> {
                Map map = Map.class.cast(obj);
                return new IndexConfiguration((String) map.get("name"), (String) map.get("type"), (List<String>) map
                    .get("fields"), (Boolean) map.get("unique"));
            }).collect(toList());
            this.indexConfigurations.put(schemaType, indexConfigurations);
        }

    }

}
