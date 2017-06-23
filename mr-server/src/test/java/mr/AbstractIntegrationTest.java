package mr;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BulkWriteResult;
import com.mongodb.WriteResult;
import io.restassured.RestAssured;
import mr.conf.MetadataAutoConfiguration;
import mr.model.MetaData;
import mr.mongo.MongobeeConfiguration;
import mr.repository.MetaDataRepository;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Override the @WebIntegrationTest annotation if you don't want to have mock shibboleth headers (e.g. you want to
 * impersonate EB or other identity).
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.data.mongodb.uri=mongodb://localhost:27017/metadata_test")
@ActiveProfiles("dev")
public abstract class AbstractIntegrationTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    @Autowired
    protected MetaDataRepository metaDataRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private MetadataAutoConfiguration metadataAutoConfiguration;

    @LocalServerPort
    protected int port;

    private static List<MetaData> metaDataList;

    @Before
    public void before() throws Exception {
        RestAssured.port = port;
        if (insertSeedData()) {
            if (metaDataList == null) {
                metaDataList = objectMapper.readValue(fileContent("json/meta_data_seed.json"), new TypeReference<List<MetaData>>() {
                });
            }
            MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
            metadataAutoConfiguration.schemaNames().forEach(schema -> {
                int removed = mongoTemplate.remove(new Query(Criteria.where("type").is(schema)), schema).getN();
                String revisionsSchema = schema.concat(MongobeeConfiguration.REVISION_POSTFIX);
                int removedRevisions = mongoTemplate.remove(new Query(Criteria.where("type").is(revisionsSchema)), revisionsSchema).getN();
                LOG.info("Removed {} records from {} and removed {} records from {}",removed, schema, removedRevisions, revisionsSchema);
            });
            metaDataList.forEach(metaDataRepository::save);
        }
    }

    protected String fileContent(String file) throws IOException {
        return StreamUtils.copyToString(new ClassPathResource(file).getInputStream(), Charset.forName("UTF-8"));
    }

    protected boolean insertSeedData() {
        return true;
    }
}
