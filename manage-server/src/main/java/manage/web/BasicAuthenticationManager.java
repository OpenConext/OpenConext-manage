package manage.web;

import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import manage.shibboleth.FederatedUser;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.List;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

/**
 * Internal APIs are trusted clients
 */
public class BasicAuthenticationManager implements AuthenticationManager {

    private final String userName;
    private final String password;
    private final List<Features> featureToggles;
    private final Product product;
    private final Push push;


    public BasicAuthenticationManager(String userName, String password, List<Features> featureToggles, Product product, Push push) {
        Assert.notNull(userName, "userName is required");
        Assert.notNull(password, "password is required");

        this.userName = userName;
        this.password = password;
        this.featureToggles = featureToggles;
        this.product = product;
        this.push = push;
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
        return new UsernamePasswordAuthenticationToken(
            new FederatedUser(
                name,
                name,
                name,
                createAuthorityList("ROLE_USER", "ROLE_ADMIN"),
                featureToggles,
                product,
                push
            ), authentication.getCredentials(), createAuthorityList("ROLE_USER", "ROLE_ADMIN"));
    }
}
