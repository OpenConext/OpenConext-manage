package manage.mongo;

import io.mongock.driver.mongodb.springdata.v4.SpringDataMongoV4Driver;
import io.mongock.runner.springboot.MongockSpringboot;
import io.mongock.runner.springboot.base.MongockApplicationRunner;
import manage.conf.MetaDataAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.Arrays;

@Configuration
public class MongoConfiguration {


    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(new EpochConverter()));
    }

    @Bean
    public MongockApplicationRunner mongockApplicationRunner(ApplicationContext springContext,
                                                                            MetaDataAutoConfiguration metaDataAutoConfiguration,
                                                                            MongoTemplate mongoTemplate) {
        SpringDataMongoV4Driver driver = SpringDataMongoV4Driver.withDefaultLock(mongoTemplate);
        driver.disableTransaction();

        return MongockSpringboot.builder()
                .setDriver(driver)
                .addMigrationScanPackage("manage.mongo")
                .addDependency(metaDataAutoConfiguration)
                .setSpringContext(springContext)
                .buildApplicationRunner();
    }

    @Bean
    public MongoTransactionManager transactionManager(MongoTemplate mongoTemplate) {
        return new MongoTransactionManager(mongoTemplate.getMongoDatabaseFactory());
    }



}
