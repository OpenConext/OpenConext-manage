package mr.web;

import mr.shibboleth.ShibbolethPreAuthenticatedProcessingFilter;
import mr.shibboleth.ShibbolethUserDetailService;
import mr.shibboleth.mock.MockShibbolethFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

/**
 * Protect endpoints for the internal JS API with Shibboleth AbstractPreAuthenticatedProcessingFilter.
 * <p>
 * Protect the internal endpoints for other Server applications with basic authentication.
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

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http
                .requestMatchers().antMatchers("/client/**")
                .and()
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .and()
                .csrf()
                .requireCsrfProtectionMatcher(new CsrfProtectionMatcher())
                .and()
                .addFilterAfter(new CsrfTokenResponseHeaderBindingFilter(), CsrfFilter.class)
                .addFilterBefore(new SessionAliveFilter(), CsrfFilter.class)
                .addFilterBefore(
                    new ShibbolethPreAuthenticatedProcessingFilter(authenticationManagerBean()),
                    AbstractPreAuthenticatedProcessingFilter.class
                )
                .authorizeRequests()
                .antMatchers("/client/**").hasRole("USER");

            if (environment.acceptsProfiles("no-csrf")) {
                http.csrf().disable();
            }
            if (environment.acceptsProfiles("dev", "no-csrf")) {
                //we can't use @Profile, because we need to add it before the real filter
                http.addFilterBefore(new MockShibbolethFilter(), ShibbolethPreAuthenticatedProcessingFilter.class);
            }
        }
    }

    @Configuration
    @Order
    public static class SecurityConfigurationAdapter extends WebSecurityConfigurerAdapter  {

        @Value("${security.internal_user_name}")
        private String user;

        @Value("${security.internal_password}")
        private String password;

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.ignoring().antMatchers("/health", "/info");
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http
                .antMatcher("/**")
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .csrf()
                .disable()
                .addFilterBefore(
                    new BasicAuthenticationFilter(
                        new BasicAuthenticationManager(user, password)),
                    BasicAuthenticationFilter.class
                )
                .authorizeRequests()
                .antMatchers("/internal/**").hasRole("ADMIN")
                .antMatchers("/**").hasRole("USER");
        }

    }

    @Configuration
    public class MvcConfig extends WebMvcConfigurerAdapter {

        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
            argumentResolvers.add(new FederatedUserHandlerMethodArgumentResolver());
        }

    }

}
