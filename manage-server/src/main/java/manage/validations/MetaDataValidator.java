package manage.validations;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@Component
public class MetaDataValidator {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataValidator.class);

    private MetaDataRepository metaDataRepository;
    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    @Autowired
    public MetaDataValidator(MetaDataRepository metaDataRepository, MetaDataAutoConfiguration
        metaDataAutoConfiguration) {
        this.metaDataRepository = metaDataRepository;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    public Map<String, Object> validateMigration() {
        Map<String, Object> results = new HashMap<>();
        Stream.of(EntityType.values()).map(EntityType::getType).forEach(type -> {
            metaDataRepository.getMongoTemplate().findAll(MetaData.class, type)
                .stream().forEach(metaData -> this.validate(metaData, type, results));
        });
        return results;
    }

    private void validate(MetaData metaData, String type, Map<String, Object> results) {
        if (Map.class.cast(metaData.getData()).get("state").equals("testaccepted")) {
            //we are only interested in invalid prodaccepted states
            return;
        }
        try {
            String json = metaDataAutoConfiguration.getObjectMapper().writeValueAsString(metaData.getData());
            metaDataAutoConfiguration.validate(json, type);
        } catch (ValidationException e) {
            Map data = Map.class.cast(metaData.getData());
            Map<String, Object> resultsMap = e.toJSON().toMap();
            LOG.info("ValidationException for id {} eid {} entityId {} type {} with exception {}",
                data.get("id"), data.get("eid"), data.get("entityid"), type, resultsMap);
            results.put(String.class.cast(data.get("entityid")), resultsMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

}
