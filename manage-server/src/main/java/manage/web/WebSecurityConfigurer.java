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
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
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
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.yaml.snakeyaml.Yaml;

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
@EnableGlobalMethodSecurity(prePostEnabled = true)
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

        @Value("${security.backdoor_user_name}")
        private String user;

        @Value("${security.backdoor_password}")
        private String password;

        @Value("${push.url}")
        private String pushUrl;

        @Value("${push.name}")
        private String pushName;

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.ignoring().antMatchers("/client/users/disclaimer");
        }

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            List<Features> featuresList = Stream.of(features.split(","))
                .map(feature -> Features.valueOf(feature.trim().toUpperCase()))
                .collect(toList());
            Product product = new Product(productOrganization, productName);
            Push push = new Push(pushUrl, pushName);

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
                    new ShibbolethPreAuthenticatedProcessingFilter(authenticationManagerBean(), featuresList,
                        product, push),
                    AbstractPreAuthenticatedProcessingFilter.class
                )
                .addFilterBefore(
                    new BasicAuthenticationFilter(
                        new BasicAuthenticationManager(user, password, featuresList, product, push)),
                    ShibbolethPreAuthenticatedProcessingFilter.class
                )
                .authorizeRequests()
                .antMatchers("/client/**").hasRole("USER");

            if (environment.acceptsProfiles("dev")) {
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

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.ignoring().antMatchers("/health", "/info");
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            APIUserConfiguration apiUserConfiguration = new Yaml()
                .loadAs(resourceLoader.getResource(configApiUsersFileLocation).getInputStream(), APIUserConfiguration
                    .class);
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
    public class MvcConfig extends WebMvcConfigurerAdapter {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new FederatedUserHandlerMethodArgumentResolver());
            argumentResolvers.add(new APIUserHandlerMethodArgumentResolver());
        }

    }

}
