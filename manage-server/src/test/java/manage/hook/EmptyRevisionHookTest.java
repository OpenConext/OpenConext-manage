package manage.hook;

import manage.TestUtils;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EmptyRevisionHookTest implements TestUtils {

    private EmptyRevisionHook subject = new EmptyRevisionHook(new MetaDataAutoConfiguration(
            objectMapper,
            new ClassPathResource("metadata_configuration"),
            new ClassPathResource("metadata_templates")));

    public EmptyRevisionHookTest() throws IOException {
    }

    @Test
    public void appliesForMetaData() {
        Stream.of(EntityType.values())
                .forEach(entityType -> assertTrue(subject.appliesForMetaData(new MetaData(entityType.getType(), emptyMap()))));
        assertTrue(subject.appliesForMetaData(null));
    }

    @Test
    public void prePutNotChanged() throws IOException {
        MetaData prevMetaData = readMetaData();
        MetaData newMetaData = readMetaData();

        newMetaData.getData().put("revisionnote", "has changed");
        assertThrows(ValidationException.class, () -> subject.prePut(prevMetaData, newMetaData, apiUser()));
    }

    @Test
    public void prePutNullNotChanged() throws IOException {
        MetaData prevMetaData = readMetaData();
        MetaData newMetaData = readMetaData();
        prevMetaData.getData().put("notes", null);
        newMetaData.getData().put("notes", null);

        newMetaData.getData().put("revisionnote", "has changed");
        assertThrows(ValidationException.class, () -> subject.prePut(prevMetaData, newMetaData,apiUser()));
    }

    @Test
    public void prePutNullChanged() throws IOException {
        MetaData prevMetaData = readMetaData();
        MetaData newMetaData = readMetaData();
        prevMetaData.getData().put("notes", "changed");
        newMetaData.getData().put("notes", null);

        newMetaData.getData().put("revisionnote", "has changed");
        subject.prePut(prevMetaData, newMetaData, apiUser());
    }

    @Test
    public void prePutNullPrevChanged() throws IOException {
        MetaData prevMetaData = readMetaData();
        MetaData newMetaData = readMetaData();
        prevMetaData.getData().put("notes", null);
        newMetaData.getData().put("notes", "changed");

        newMetaData.getData().put("revisionnote", "has changed");
        subject.prePut(prevMetaData, newMetaData, apiUser());
    }

    @Test
    public void prePut() throws IOException {
        MetaData prevMetaData = readMetaData();
        MetaData newMetaData = readMetaData();
        prevMetaData.metaDataFields().put("coin:institution_id", UUID.randomUUID().toString());

        newMetaData.getData().put("revisionnote", "has changed");
        subject.prePut(prevMetaData, newMetaData,apiUser() );
    }

    private MetaData readMetaData() throws IOException {
        return objectMapper.readValue(new ClassPathResource("json/meta_data_detail.json").getInputStream(), MetaData.class);
    }


}