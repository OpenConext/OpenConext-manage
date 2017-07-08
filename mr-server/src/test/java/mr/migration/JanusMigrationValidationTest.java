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

    private int count = 0;

    @Test
    public void validateMigrate() throws Exception {
        metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_idp_revision")
            .stream().forEach(metaData -> this.validate(metaData, "saml20_idp"));
        System.out.println(count);
    }

    private void validate(MetaData metaData, String type) {
        try {
            String json = objectMapper.writeValueAsString(metaData.getData());
            metaDataAutoConfiguration.validate(json, type);
        } catch (ValidationException e) {
            ++this.count;
//            Map data = Map.class.cast(metaData.getData());
//            LOG.info("ValidationException for id {} eid {} entityId {} with exception {}",
//                data.get("id"), data.get("eid"), data.get("entityid"), e.toJSON().toMap());
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected boolean insertSeedData() {
        return false;
    }


}