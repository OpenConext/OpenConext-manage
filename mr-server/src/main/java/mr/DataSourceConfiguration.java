package mr;

import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfiguration {

    @Bean("dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource srDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean("ebDataSource")
    @ConfigurationProperties(prefix = "eb.datasource")
    public DataSource ebDataSource() {
        return DataSourceBuilder.create().build();
    }
}
