package mr.web;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

/**
 * Internal APIs are trusted clients
 */
public class BasicAuthenticationManager implements AuthenticationManager {

    private final String userName;
    private final String password;

    public BasicAuthenticationManager(String userName, String password) {
        Assert.notNull(userName, "userName is required");
        Assert.notNull(password, "password is required");

        this.userName = userName;
        this.password = password;
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
        return new UsernamePasswordAuthenticationToken(
            authentication.getPrincipal(),
            authentication.getCredentials(),
            createAuthorityList("ROLE_USER", "ROLE_ADMIN"));
    }
}
