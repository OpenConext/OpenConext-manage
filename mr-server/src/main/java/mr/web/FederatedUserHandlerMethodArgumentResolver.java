package mr.web;

import mr.shibboleth.FederatedUser;
import org.springframework.core.MethodParameter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

public class FederatedUserHandlerMethodArgumentResolver implements
        HandlerMethodArgumentResolver {

    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(FederatedUser.class);
    }

    public FederatedUser resolveArgument(MethodParameter methodParameter,
                                         ModelAndViewContainer mavContainer,
                                         NativeWebRequest webRequest,
                                         WebDataBinderFactory binderFactory) throws Exception {
        return FederatedUser.class.cast(PreAuthenticatedAuthenticationToken.class.cast(webRequest.getUserPrincipal())
                .getPrincipal());
    }
}