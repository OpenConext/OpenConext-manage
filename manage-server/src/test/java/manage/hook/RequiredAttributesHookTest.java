package manage.hook;

import manage.TestUtils;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequiredAttributesHookTest implements TestUtils {

    private RequiredAttributesHook subject = new RequiredAttributesHook(new MetaDataAutoConfiguration(
            objectMapper,
            new ClassPathResource("metadata_configuration"),
            new ClassPathResource("metadata_templates")));

    public RequiredAttributesHookTest() throws IOException {
    }

    @Test
    void appliesForMetaData() {
        Stream.of(EntityType.values())
                .forEach(entityType -> assertTrue(subject.appliesForMetaData(new MetaData(entityType.getType(), emptyMap()))));
        assertTrue(subject.appliesForMetaData(null));
    }

    @Test
    void prePut() throws Throwable {
        MetaData prevMetaData = readMetaData();
        MetaData newMetaData = readMetaData();
        Executable executable = () -> subject.prePut(prevMetaData, newMetaData);
        doExecute(newMetaData, executable);
    }

    @Test
    void prePost() throws Throwable {
        MetaData newMetaData = readMetaData();
        Executable executable = () -> subject.prePost(newMetaData);
        doExecute(newMetaData, executable);
    }

    private void doExecute(MetaData newMetaData, Executable executable) throws Throwable {
        Assertions.assertThrows(ValidationException.class, executable);
        Map<String, Object> metaDataFields = newMetaData.metaDataFields();

        metaDataFields.put("coin:stepup:allow_no_token", true);
        Assertions.assertThrows(ValidationException.class, executable);

        metaDataFields.put("OrganizationDisplayName:en", "displayName");
        metaDataFields.put("OrganizationURL:en", "https://org.nl");
        executable.execute();

        metaDataFields.remove("coin:stepup:requireloa");
        metaDataFields.remove("coin:stepup:allow_no_token");
        executable.execute();
    }

    private MetaData readMetaData() throws IOException {
        return objectMapper.readValue(new ClassPathResource("json/meta_data_missing_required_attributes.json").getInputStream(), MetaData.class);
    }


}