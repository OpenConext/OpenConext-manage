package mr.api;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.stream.Collectors;

public class APIAuthenticationManager implements AuthenticationManager {

    private APIUserConfiguration apiUserConfiguration;

    public APIAuthenticationManager(APIUserConfiguration apiUserConfiguration) {
        this.apiUserConfiguration = apiUserConfiguration;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = String.class.cast(authentication.getPrincipal());
        Optional<APIUser> apiUserOptional = apiUserConfiguration.getApiUsers().stream()
            .filter(apiUser -> apiUser.getName().equals(name)).findFirst();
        APIUser apiUser = apiUserOptional.orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + name));
        if (!apiUser.getPassword().equals(authentication.getCredentials())) {
            throw new BadCredentialsException("Bad credentials");
        }
        return new UsernamePasswordAuthenticationToken(
            apiUser,
            authentication.getCredentials(),
            apiUser.getScopes().stream().map(scope -> new SimpleGrantedAuthority("ROLE_".concat(scope.name())))
                .collect(Collectors.toList()));
    }
}
