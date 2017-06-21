package mr.shibboleth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

public class ShibbolethPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    public static final String NAME_ID_HEADER_NAME = "name-id";
    public static final String DISPLAY_NAME_HEADER_NAME = "displayname";
    public static final String SCHAC_HOME_HEADER = "schacHomeOrganization";

    public ShibbolethPreAuthenticatedProcessingFilter(AuthenticationManager authenticationManager) {
        super();
        setAuthenticationManager(authenticationManager);
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
        String uid = getHeader(NAME_ID_HEADER_NAME, request);
        String displayName = getHeader(DISPLAY_NAME_HEADER_NAME, request);
        String schacHomeOrganization = getHeader(SCHAC_HOME_HEADER, request);

        if (StringUtils.isEmpty(uid) || StringUtils.isEmpty(displayName)) {
            //this is the contract. See AbstractPreAuthenticatedProcessingFilter#doAuthenticate
            return null;
        }
        return new FederatedUser(uid, displayName, schacHomeOrganization, createAuthorityList("ROLE_USER", "ROLE_ADMIN"));
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "N/A";
    }

    private String getHeader(String name, HttpServletRequest request) {
        String header = request.getHeader(name);
        try {
            return StringUtils.hasText(header) ?
                new String(header.getBytes("ISO8859-1"), "UTF-8") : header;
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }


}
