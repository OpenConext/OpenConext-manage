package manage.shibboleth;

import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Push push;
    private static final Logger LOG = LoggerFactory.getLogger(ShibbolethPreAuthenticatedProcessingFilter.class);

    public ShibbolethPreAuthenticatedProcessingFilter(AuthenticationManager authenticationManager, List<Features>
            featureToggles, Product product, Push push) {
        super();
        setAuthenticationManager(authenticationManager);
        this.featureToggles = featureToggles;
        this.product = product;
        this.push = push;
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
        String uid = getHeader(NAME_ID_HEADER_NAME, request);
        String displayName = getHeader(DISPLAY_NAME_HEADER_NAME, request);
        String schacHomeOrganization = getHeader(SCHAC_HOME_HEADER, request);

        if (StringUtils.isEmpty(uid) || StringUtils.isEmpty(displayName)) {
            //this is the contract. See AbstractPreAuthenticatedProcessingFilter#doAuthenticate
            LOG.error("Missing required attribute(s): uid {} displayName {}", uid, displayName);
            return null;
        }
        List<GrantedAuthority> authorityList = createAuthorityList("ROLE_USER", "ROLE_ADMIN");
        return new FederatedUser(uid, displayName, schacHomeOrganization,
                authorityList, featureToggles, product, push);
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
