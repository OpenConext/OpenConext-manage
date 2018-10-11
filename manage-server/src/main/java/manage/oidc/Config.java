package manage.oidc;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public OpenIdConnect openIdConnect(@Value("${oidc.feature}") boolean oidc,
                                       @Value("${oidc.user}") String user,
                                       @Value("${oidc.password}") String password,
                                       @Value("${oidc.url}") String url) {
        return oidc ? new OpenIdConnectService(user, password, url) : new OpenIdConnectMock();
    }

}
