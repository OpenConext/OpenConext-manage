package manage;

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
        DataSource dataSource = DataSourceBuilder.create().build();
        if (dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
            return tomcatDataSource(dataSource);
        }
        throw new IllegalArgumentException("Expected tomcat DataSource. Got " + dataSource.getClass().getName());
    }

    private DataSource tomcatDataSource(DataSource dataSource) {
        org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = (org.apache.tomcat.jdbc.pool.DataSource) dataSource;
        tomcatDataSource.setTestOnBorrow(true);
        tomcatDataSource.setValidationQuery("SELECT 1");
        tomcatDataSource.setRemoveAbandoned(true);
        tomcatDataSource.setTestWhileIdle(true);
        tomcatDataSource.setLogValidationErrors(true);
        return tomcatDataSource;
    }

    @Bean("ebDataSource")
    @ConfigurationProperties(prefix = "eb.datasource")
    public DataSource ebDataSource() {
        DataSource dataSource = DataSourceBuilder.create().build();
        if (dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
            return tomcatDataSource(dataSource);
        }
        throw new IllegalArgumentException("Expected tomcat DataSource. Got " + dataSource.getClass().getName());
    }
}
