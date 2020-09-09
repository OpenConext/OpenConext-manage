package manage.mongo;

import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.SpringDataMongo3Driver;
import com.github.cloudyrock.spring.v5.MongockSpring5;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Configuration
public class MongoConfiguration {

    @Autowired
    private MappingMongoConverter mongoConverter;

    @Bean
    public CustomConversions customConversions() {
        return new CustomConversions(Arrays.asList(new EpochConverter()));
    }

    // Converts . into a mongo friendly char
    @PostConstruct
    public void setUpMongoEscapeCharacterConversion() {
        mongoConverter.setMapKeyDotReplacement("@");
    }

    @Bean
    public MongockSpring5.MongockApplicationRunner mongockApplicationRunner(ApplicationContext springContext,
                                                                            MongoTemplate mongoTemplate) {
        SpringDataMongo3Driver driver = SpringDataMongo3Driver.withDefaultLock(mongoTemplate);
        driver.disableTransaction();

        return MongockSpring5.builder()
                .setDriver(driver)
                .addChangeLogsScanPackage("oidc.mongo")
                .setSpringContext(springContext)
                .buildApplicationRunner();
    }

}
