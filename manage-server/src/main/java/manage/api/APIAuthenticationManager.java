package manage.api;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.stream.Collectors;

public class APIAuthenticationManager implements AuthenticationManager {

    //trusted API headers
    public static final String X_IDP_ENTITY_ID = "X-IDP-ENTITY-ID";
    public static final String X_UNSPECIFIED_NAME_ID = "X-UNSPECIFIED-NAME-ID";
    public static final String X_DISPLAY_NAME = "X-DISPLAY-NAME";

    private final APIUserConfiguration apiUserConfiguration;

    public APIAuthenticationManager(APIUserConfiguration apiUserConfiguration) {
        this.apiUserConfiguration = apiUserConfiguration;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String name = String.class.cast(authentication.getPrincipal());
        Optional<APIUser> apiUserOptional = apiUserConfiguration.getApiUsers().stream()
                .filter(apiUser -> apiUser.getName().equals(name))
                .findFirst();
        APIUser apiUser = apiUserOptional.orElseThrow(() -> new UsernameNotFoundException("Unknown user: " + name));
        if (!apiUser.getPassword().equals(authentication.getCredentials())) {
            throw new BadCredentialsException("Bad credentials");
        }

        APIUser principal = new APIUser(apiUser);
        Optional<ImpersonatedUser> impersonatedUserOptional = impersonatedUser();
        impersonatedUserOptional.ifPresent(principal::setImpersonatedUser);

        return new UsernamePasswordAuthenticationToken(
                principal,
                authentication.getCredentials(),
                apiUser.getScopes().stream()
                        .map(scope -> new SimpleGrantedAuthority("ROLE_".concat(scope.name())))
                        .collect(Collectors.toList()));
    }

    private Optional<ImpersonatedUser> impersonatedUser() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String idpEntityId = request.getHeader(X_IDP_ENTITY_ID);
        String unspecifiedNameId = request.getHeader(X_UNSPECIFIED_NAME_ID);
        String displayName = request.getHeader(X_DISPLAY_NAME);
        if (StringUtils.hasText(idpEntityId) && StringUtils.hasText(unspecifiedNameId) && StringUtils.hasText(displayName)) {
            return Optional.of(new ImpersonatedUser(idpEntityId, unspecifiedNameId, displayName));
        }
        return Optional.empty();
    }

}
