package manage;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import lombok.SneakyThrows;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import manage.repository.ScopeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;
import static manage.mongo.MongoChangelog.REVISION_POSTFIX;
import static org.awaitility.Awaitility.await;

/**
 * Override the @WebIntegrationTest annotation if you don't want to have mock shibboleth headers (e.g. you want to
 * impersonate EB or other identity).
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = "classpath:test.properties")
public abstract class AbstractIntegrationTest implements TestUtils {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    @Autowired
    protected MetaDataRepository metaDataRepository;

    @Autowired
    protected ScopeRepository scopeRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected MetaDataAutoConfiguration metaDataAutoConfiguration;

    @LocalServerPort
    protected int port;

    private static List<MetaData> metaDataList;

    @BeforeEach
    public void before() throws Exception {
        RestAssured.port = port;
        if (insertSeedData()) {
            if (metaDataList == null) {
                metaDataList = objectMapper.readValue(readFile("json/meta_data_seed.json"),
                        new TypeReference<>() {
                        });
            }
            MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
            Query query = new Query();
            metaDataAutoConfiguration.schemaNames().forEach(schema -> {
                long removed = mongoTemplate.remove(query, schema).getDeletedCount();
                String revisionsSchema = schema.concat(REVISION_POSTFIX);
                long removedRevisions = mongoTemplate.remove(query, revisionsSchema).getDeletedCount();
                LOG.debug("Removed {} records from {} and removed {} records from {}", removed, schema,
                        removedRevisions, revisionsSchema);
            });
            metaDataList.forEach(metaDataRepository::save);

            metaDataList.stream().collect(Collectors.groupingBy(MetaData::getType))
                    .forEach((type, metaData) -> await().until(() -> mongoTemplate.count(query, type) == metaData
                            .size()));
            Stream.of(EntityType.values()).forEach(entityType -> mongoTemplate.remove(query, entityType.getType().concat(CHANGE_REQUEST_POSTFIX)));
        }
    }

    @SneakyThrows
    protected Map<String, Object> readValueFromFile(String path) {
        return objectMapper.readValue(readFile(path), new TypeReference<>() {
        });
    }

    protected boolean insertSeedData() {
        return true;
    }

    protected MongoTemplate mongoTemplate() {
        return metaDataRepository.getMongoTemplate();
    }
}
