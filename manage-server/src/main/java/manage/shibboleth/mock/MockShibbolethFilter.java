package manage.shibboleth.mock;

import manage.shibboleth.ShibbolethPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.util.HashMap;

public class MockShibbolethFilter extends GenericFilterBean {

    public static final String SAML2_USER = "saml2_user.com";

    private static class SetHeader extends HttpServletRequestWrapper {

        private final HashMap<String, String> headers;

        public SetHeader(HttpServletRequest request) {
            super(request);
            this.headers = new HashMap<>();
        }

        public void setHeader(String name, String value) {
            this.headers.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            if (headers.containsKey(name)) {
                return headers.get(name);
            }
            return super.getHeader(name);
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        SetHeader wrapper = new SetHeader((HttpServletRequest) servletRequest);
        wrapper.setHeader(ShibbolethPreAuthenticatedProcessingFilter.NAME_ID_HEADER_NAME, SAML2_USER);
        wrapper.setHeader(ShibbolethPreAuthenticatedProcessingFilter.DISPLAY_NAME_HEADER_NAME, "John Doe");
        wrapper.setHeader(ShibbolethPreAuthenticatedProcessingFilter.SCHAC_HOME_HEADER, "http://mock-idp");
        filterChain.doFilter(wrapper, servletResponse);
    }
}
