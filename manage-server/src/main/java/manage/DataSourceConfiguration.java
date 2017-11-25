package manage;

import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.Assert;

import java.sql.SQLException;


@Configuration
public class DataSourceConfiguration {

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties srDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("dataSource")
    @Primary
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource srDataSource() throws SQLException {
        return initDataSource(srDataSourceProperties());

    }

    @Bean
    @ConfigurationProperties("eb.datasource")
    public DataSourceProperties ebDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean("ebDataSource")
    @ConfigurationProperties(prefix = "eb.datasource")
    public DataSource ebDataSource() {
        return initDataSource(ebDataSourceProperties());
    }

    private DataSource initDataSource(DataSourceProperties dataSourceProperties) {
        DataSource dataSource = (DataSource) dataSourceProperties.initializeDataSourceBuilder().type(DataSource.class)
            .build();
        setTypeSpecificProperties(dataSource);
        return dataSource;
    }

    private void setTypeSpecificProperties(DataSource tomcatDataSource) {
        tomcatDataSource.setTestOnBorrow(true);
        tomcatDataSource.setValidationQuery("SELECT 1");
        tomcatDataSource.setRemoveAbandoned(true);
        tomcatDataSource.setTestWhileIdle(true);
        tomcatDataSource.setLogValidationErrors(true);

        ConnectionPool pool = tomcatDataSource.getPool();
        Assert.notNull(pool, "Tomcat datasource pool is null");

    }
}
