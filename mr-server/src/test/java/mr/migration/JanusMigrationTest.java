package mr.migration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fakemongo.Fongo;
import mr.TestUtils;
import mr.conf.MetaDataAutoConfiguration;
import mr.model.MetaData;
import mr.repository.MetaDataRepository;
import org.apache.commons.dbcp.BasicDataSource;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JanusMigrationTest implements TestUtils {

    private JanusMigration subject;
    private MetaDataRepository metaDataRepository;
    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    @Before
    public void before() throws Exception {
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setUrl("jdbc:h2:mem:testdb;MODE=MySQL;");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        jdbcTemplate.execute(readFile("sql/migration_seed.sql"));

        Fongo fongo = new Fongo("test");
        MongoTemplate mongoTemplate = new MongoTemplate(fongo.getMongo(), "test");
        metaDataRepository = new MetaDataRepository(mongoTemplate);

        this.metaDataAutoConfiguration = new MetaDataAutoConfiguration(
            objectMapper, new ClassPathResource("metadata_configuration"), new ClassPathResource("metadata_templates")
        );
        this.subject = new JanusMigration("key_column", dataSource, mongoTemplate, metaDataAutoConfiguration);
    }

    @Test
    public void migrate() throws Exception {
        subject.doMigrate();

        List<MetaData> identityProviders = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_idp");
        List<MetaData> serviceProviders = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_sp");

        assertEquals(2, identityProviders.size());
        assertEquals(2, serviceProviders.size());

        JanusMigrationValidation validation = new JanusMigrationValidation(metaDataRepository, metaDataAutoConfiguration);
        Map<String, Object> validations = validation.validateMigration();

        assertEquals(0, validations.size());
    }

}