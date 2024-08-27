package manage.web;

import manage.api.APIAuthenticationManager;
import manage.api.APIUserConfiguration;
import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import manage.shibboleth.ShibbolethPreAuthenticatedProcessingFilter;
import manage.shibboleth.ShibbolethUserDetailService;
import manage.shibboleth.mock.MockShibbolethFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Protect endpoints for the internal JS API with Shibboleth AbstractPreAuthenticatedProcessingFilter.
 * <p>
 * Protect the internal endpoints for other Server applications with basic authentications.
 * <p>
 * Do not protect public endpoints like /health and /info
 */
@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class WebSecurityConfigurer {

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        //because Autowired this will end up in the global ProviderManager
        PreAuthenticatedAuthenticationProvider authenticationProvider = new PreAuthenticatedAuthenticationProvider();
        authenticationProvider.setPreAuthenticatedUserDetailsService(new ShibbolethUserDetailService());
        auth.authenticationProvider(authenticationProvider);
    }

    @Order(1)
    @Configuration
    public static class InternalSecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Autowired
        private Environment environment;

        @Value("${features}")
        private String features;

        @Value("${product.organization}")
        private String productOrganization;

        @Value("${product.name}")
        private String productName;

        @Value("${product.service_provider_feed_url}")
        private String serviceProviderFeedUrl;

        @Value("${product.show_oidc_rp}")
        private boolean showOidcRp;

        @Value("${security.backdoor_user_name}")
        private String user;

        @Value("${security.backdoor_password}")
        private String password;

        @Value("${push.eb.url}")
        private String pushUrl;

        @Value("${push.eb.name}")
        private String pushName;

        @Value("${push.eb.exclude_oidc_rp}")
        private boolean excludeOidcRP;

        @Value("${push.oidc.url}")
        private String pushOidcUrl;

        @Value("${push.oidc.name}")
        private String pushOidcName;

        @Value("${push.pdp.url}")
        private String pdpPushUri;

        @Value("${push.pdp.enabled}")
        private boolean pdpEnabled;

        @Value("${push.pdp.name}")
        private String pdpName;

        @Value("${security.super_user_team_names}")
        private String superUserTeamNamesJoined;

        @Value("${environment}")
        private String environmentType;

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.ignoring().antMatchers("/client/users/disclaimer");
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            List<String> allFeatures = Arrays.asList(Features.values()).stream()
                    .map(Enum::name)
                    .collect(toList());

            List<Features> featuresList = Stream.of(this.features.split(","))
                    .filter(feature -> allFeatures.contains(feature.trim().toUpperCase()))
                    .map(feature -> Features.valueOf(feature.trim().toUpperCase()))
                    .collect(toList());

            Product product = new Product(productOrganization, productName, serviceProviderFeedUrl, showOidcRp);
            Push push = new Push(pushUrl, pushName, pushOidcUrl, pushOidcName, pdpPushUri,pdpName, excludeOidcRP, pdpEnabled);

            BasicAuthenticationEntryPoint authenticationEntryPoint = new BasicAuthenticationEntryPoint();
            authenticationEntryPoint.setRealmName("manage");

            http
                    .requestMatchers().antMatchers("/client/**")
                    .and()
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .and()
                    .csrf()
                    .requireCsrfProtectionMatcher(new CsrfProtectionMatcher())
                    .and()
                    .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint)
                    .and()
                    .addFilterAfter(new CsrfTokenResponseHeaderBindingFilter(), CsrfFilter.class)
                    .addFilterBefore(new SessionAliveFilter(), CsrfFilter.class)
                    .addFilterBefore(
                            new ShibbolethPreAuthenticatedProcessingFilter(
                                    authenticationManagerBean(),
                                    featuresList,
                                    product,
                                    push,
                                    superUserTeamNamesJoined,
                                    environmentType),
                            AbstractPreAuthenticatedProcessingFilter.class
                    )
                    .addFilterBefore(
                            new BasicAuthenticationFilter(
                                    new BasicAuthenticationManager(user, password, featuresList, product, push, environmentType)),
                            ShibbolethPreAuthenticatedProcessingFilter.class
                    )
                    .authorizeRequests()
                    .antMatchers("/client/**").hasRole("ADMIN");

            if (environment.acceptsProfiles(Profiles.of("dev"))) {
                //we can't use @Profile, because we need to add it before the real filter
                http.csrf().disable();
                http.addFilterBefore(new MockShibbolethFilter(), ShibbolethPreAuthenticatedProcessingFilter.class);
            }
        }
    }

    @Configuration
    @Order
    public static class SecurityConfigurationAdapter extends WebSecurityConfigurerAdapter {

        @Value("${security.api_users_config_path}")
        private String configApiUsersFileLocation;

        @Autowired
        private ResourceLoader resourceLoader;

        @Value("${environment}")
        private String environmentType;

        @Override
        public void configure(WebSecurity web) {
            web.ignoring().antMatchers("/internal/health", "/internal/info");
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            APIUserConfiguration apiUserConfiguration = new Yaml()
                    .loadAs(resourceLoader.getResource(configApiUsersFileLocation).getInputStream(), APIUserConfiguration
                            .class);
            apiUserConfiguration.getApiUsers().forEach(apiUser -> apiUser.setEnvironment(environmentType));
            http
                    .antMatcher("/internal/**")
                    .sessionManagement()
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                    .and()
                    .csrf()
                    .disable()
                    .addFilterBefore(
                            new BasicAuthenticationFilter(
                                    new APIAuthenticationManager(apiUserConfiguration)
                            ), BasicAuthenticationFilter.class
                    )
                    .authorizeRequests()
                    .antMatchers("/internal/**").hasRole("READ");
        }

    }

    @Configuration
    @EnableScheduling
    public class MvcConfig implements WebMvcConfigurer {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new FederatedUserHandlerMethodArgumentResolver());
            argumentResolvers.add(new APIUserHandlerMethodArgumentResolver());
        }

    }

    @Bean
    public HttpFirewall httpFirewall() {
        return new DefaultHttpFirewall();
    }

}
