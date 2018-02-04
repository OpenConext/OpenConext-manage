package manage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.MetricFilterAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.TraceWebFilterAutoConfiguration;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.HealthMvcEndpoint;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

@SpringBootApplication(exclude = {TraceWebFilterAutoConfiguration.class, MetricFilterAutoConfiguration.class})
@EnableMongoRepositories(basePackages = "manage.repository")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public HealthMvcEndpoint exposeDetailsHealthMvcEndpoint(HealthEndpoint delegate) {
        return new HealthMvcEndpoint(delegate) {

            @Override
            protected boolean exposeHealthDetails(HttpServletRequest request, Principal principal) {
                return true;
            }

        };
    }
}

