package manage.web;

import manage.shibboleth.FederatedUser;
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

    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(FederatedUser.class);
    }

    public FederatedUser resolveArgument(MethodParameter methodParameter,
                                         ModelAndViewContainer mavContainer,
                                         NativeWebRequest webRequest,
                                         WebDataBinderFactory binderFactory) throws Exception {
        Principal principal = webRequest.getUserPrincipal();
        if (principal instanceof PreAuthenticatedAuthenticationToken) {
            return FederatedUser.class.cast(PreAuthenticatedAuthenticationToken.class.cast(principal).getPrincipal());
        } else {
            return FederatedUser.class.cast(UsernamePasswordAuthenticationToken.class.cast(principal).getPrincipal());
        }
    }
}