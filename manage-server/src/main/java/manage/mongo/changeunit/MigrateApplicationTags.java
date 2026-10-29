package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import manage.model.EntityType;
import manage.model.MetaData;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.util.StringUtils;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "migrateApplicationTags", order = "017", author = "okke.harsta@surf.nl")
public class MigrateApplicationTags {

    private static final Logger LOG = LoggerFactory.getLogger(MigrateApplicationTags.class);

    @SneakyThrows
    @Execution
    public void migrateApplicationTags(MongoTemplate mongoTemplate) {
        //All of the occurrences of coin:ss:type_of_service:en must be duplicated to application_tags
        String tags = IOUtils.toString(new ClassPathResource("migration.tags.csv").getInputStream(), Charset.defaultCharset());
        String[] lines = tags.split("\\R");
        Map<String, String> mappings = new HashMap<>();
        Stream.of(lines).forEach(s -> {
            String[] splitted = s.split(",");
            mappings.put(splitted[0].trim().toLowerCase(), splitted[1].trim().toLowerCase());
        });
        List<MetaData> serviceProviders = mongoTemplate.findAll(MetaData.class, EntityType.SP.getType());
        List<MetaData> relyingParties = mongoTemplate.findAll(MetaData.class, EntityType.RP.getType());
        serviceProviders.addAll(relyingParties);
        serviceProviders.forEach(metaData -> {
            Map<String, Object> metaDataFields = metaData.metaDataFields();
            //We only need to check the English ones, as we store application_tags language agnostic
            String value = (String) metaDataFields.get("coin:ss:type_of_service:en");
            if (StringUtils.hasText(value)) {
                //can be comma separated
                List<String> applicationTags = new ArrayList<>();
                Stream.of(value.split(",")).forEach(s -> {
                    String key = s.trim().toLowerCase();
                    if (mappings.containsKey(key)) {
                        applicationTags.add(mappings.get(key));
                    }
                });
                if (!applicationTags.isEmpty()) {
                    metaDataFields.put("application_tags", applicationTags);
                    //We don't make a new revision as this is a cleanup action
                    LOG.info(String.format("Saving %s metadata type %s where coin:ss:type_of_service is duplicated in application tags",
                        metaData.getData().get("entityid"), metaData.getType()));
                    mongoTemplate.save(metaData, metaData.getType());
                }
            }
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }
}
