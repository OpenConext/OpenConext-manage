package manage.hook;

import manage.TestUtils;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.MetaData;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class TypeSafetyHookTest implements TestUtils {

    private MetaDataAutoConfiguration metaDataAutoConfiguration = new MetaDataAutoConfiguration(
            objectMapper,
            new ClassPathResource("metadata_configuration"),
            new ClassPathResource("metadata_templates"));
    private TypeSafetyHook subject = new TypeSafetyHook(metaDataAutoConfiguration);

    public TypeSafetyHookTest() throws IOException {
    }

    @Test
    public void preValidate() {
        Map<String, Object> metaDataFieldValues = new HashMap<>();
        metaDataFieldValues.put("coin:push_enabled", "1");
        metaDataFieldValues.put("logo:0:height", "1000");
        metaDataFieldValues.put("AssertionConsumerService:0:index", "1");

        MetaData metaData = subject.preValidate(metaData("saml20_sp", metaDataFieldValues));
        metaDataFieldValues = metaData.metaDataFields();


        assertEquals(true, metaDataFieldValues.get("coin:push_enabled"));
        assertEquals(1000, metaDataFieldValues.get("logo:0:height"));
        assertEquals(1, metaDataFieldValues.get("AssertionConsumerService:0:index"));
    }

    private MetaData metaData(String type, Map<String, Object> metaDataFieldValues) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> metaDataFields = new HashMap<>();
        data.put("metaDataFields", metaDataFields);

        metaDataFields.putAll(metaDataFieldValues);
        return new MetaData(type, data);
    }

}