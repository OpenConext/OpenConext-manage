package mr.migration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mr.AbstractIntegrationTest;
import mr.conf.MetaDataAutoConfiguration;
import mr.model.MetaData;
import mr.repository.MetaDataRepository;
import org.everit.json.schema.ValidationException;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;
import java.util.stream.Stream;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Ignore
public class JanusMigrationValidationTest extends AbstractIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(JanusMigrationValidationTest.class);

    @Autowired
    private MetaDataRepository metaDataRepository;

    @Autowired
    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void validateMigrate() throws Exception {
        Stream.of(EntityType.values()).map(EntityType::getType).forEach(type -> {
            metaDataRepository.getMongoTemplate().findAll(MetaData.class, type)
                .stream().forEach(metaData -> this.validate(metaData, type));
        });
    }

    private void validate(MetaData metaData, String type) {
        if (Map.class.cast(metaData.getData()).get("state").equals("testaccepted")) {
            //we are only interested in inval prodaccepted states
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(metaData.getData());
            metaDataAutoConfiguration.validate(json, type);
        } catch (ValidationException e) {
            Map data = Map.class.cast(metaData.getData());
            LOG.info("ValidationException for id {} eid {} entityId {} type {} with exception {}",
                data.get("id"), data.get("eid"), data.get("entityid"), type, e.toJSON().toMap());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected boolean insertSeedData() {
        return false;
    }


}