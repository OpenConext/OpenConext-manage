package mr.shibboleth;

import mr.conf.Features;
import mr.conf.Product;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.List;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

public class ShibbolethPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    public static final String NAME_ID_HEADER_NAME = "name-id";
    public static final String DISPLAY_NAME_HEADER_NAME = "displayname";
    public static final String SCHAC_HOME_HEADER = "schacHomeOrganization";
    private final List<Features> featureToggles;
    private final Product product;

    public ShibbolethPreAuthenticatedProcessingFilter(AuthenticationManager authenticationManager, List<Features> featureToggles, Product product) {
        super();
        setAuthenticationManager(authenticationManager);
        this.featureToggles = featureToggles;
        this.product = product;
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
        //TODO determine guest membership based on ???
        List<GrantedAuthority> authorityList = createAuthorityList("ROLE_USER", "ROLE_ADMIN"); //createAuthorityList("ROLE_USER");
        return new FederatedUser(uid, displayName, schacHomeOrganization,
            authorityList, featureToggles, product);
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
