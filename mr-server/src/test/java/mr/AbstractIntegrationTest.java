package mr;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.RestAssured;
import mr.conf.MetadataAutoConfiguration;
import mr.model.MetaData;
import mr.mongo.MongobeeConfiguration;
import mr.repository.MetaDataRepository;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.LocalServerPort;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("dev")
public abstract class AbstractIntegrationTest {

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
        if (metaDataList == null) {
            metaDataList = objectMapper.readValue(fileContent("json/meta_data_seed.json"), new TypeReference<List<MetaData>>() {
            });
        }
        MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
        metadataAutoConfiguration.schemaNames().forEach(schema -> {
            mongoTemplate.dropCollection(schema);
            mongoTemplate.dropCollection(schema.concat(MongobeeConfiguration.REVISION_POSTFIX));
        });
        metaDataList.forEach(metaDataRepository::save);
    }


    protected String fileContent(String file) throws IOException {
        return StreamUtils.copyToString(new ClassPathResource(file).getInputStream(), Charset.forName("UTF-8"));
    }
}
