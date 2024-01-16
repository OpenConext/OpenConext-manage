package manage.web;

import manage.api.Scope;
import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import manage.shibboleth.FederatedUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.List;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

/**
 * Only used for the backdoor user if for some reason manage is causing an SSO failure
 */
public class BasicAuthenticationManager implements AuthenticationManager {

    private final String userName;
    private final String password;
    private final List<Features> featureToggles;
    private final Product product;
    private final Push push;
    private final String environment;


    public BasicAuthenticationManager(String userName,
                                      String password,
                                      List<Features> featureToggles,
                                      Product product,
                                      Push push,
                                      String environment) {
        Assert.notNull(userName, "userName is required");
        Assert.notNull(password, "password is required");

        this.userName = userName;
        this.password = password;
        this.featureToggles = featureToggles;
        this.product = product;
        this.push = push;
        this.environment = environment;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        //the exceptions are for logging and are not propagated to the end user / application
        if (!userName.equals(authentication.getPrincipal())) {
            throw new UsernameNotFoundException("Unknown user: " + authentication.getPrincipal());
        }
        if (!password.equals(authentication.getCredentials())) {
            throw new BadCredentialsException("Bad credentials");
        }
        String name = String.class.cast(authentication.getPrincipal());
        List<GrantedAuthority> authorityList = createAuthorityList("ROLE_".concat(Scope.ADMIN.name()));
        return new UsernamePasswordAuthenticationToken(
                new FederatedUser(
                        name,
                        name,
                        name,
                        authorityList,
                        featureToggles,
                        product,
                        push,
                        environment
                ), authentication.getCredentials(), authorityList);
    }

}
