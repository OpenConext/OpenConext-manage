package manage.web;

import manage.shibboleth.FederatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.security.Principal;

public class FederatedUserHandlerMethodArgumentResolver implements
        HandlerMethodArgumentResolver {

    private static final Logger LOG = LoggerFactory.getLogger(FederatedUserHandlerMethodArgumentResolver.class);

    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(FederatedUser.class);
    }

    public FederatedUser resolveArgument(MethodParameter methodParameter,
                                         ModelAndViewContainer mavContainer,
                                         NativeWebRequest webRequest,
                                         WebDataBinderFactory binderFactory) {
        Principal userPrincipal = webRequest.getUserPrincipal();
        Object principal = null;
        if (userPrincipal instanceof PreAuthenticatedAuthenticationToken) {
            PreAuthenticatedAuthenticationToken token = (PreAuthenticatedAuthenticationToken) userPrincipal;
            principal = token.getPrincipal();
            if (principal instanceof FederatedUser) {
                return (FederatedUser) principal;
            }
        } else if (userPrincipal instanceof UsernamePasswordAuthenticationToken) {
            UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) userPrincipal;
            principal = token.getPrincipal();
            if (principal instanceof FederatedUser) {
                return (FederatedUser) principal;
            }
        }
        LOG.warn(String.format("Can not extract FederatedUser from " +
                        "userPrincipal %s " +
                        "name %s " +
                        "principal %s " +
                        "webRequest %s",
                userPrincipal.getClass().getCanonicalName(),
                userPrincipal.getName(),
                principal,
                webRequest.getDescription(true)
        ));
        return null;
    }
}