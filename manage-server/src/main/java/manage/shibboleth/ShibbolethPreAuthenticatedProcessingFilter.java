package manage.shibboleth;

import lombok.SneakyThrows;
import manage.api.Scope;
import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.security.core.authority.AuthorityUtils.createAuthorityList;

public class ShibbolethPreAuthenticatedProcessingFilter extends AbstractPreAuthenticatedProcessingFilter {

    public static final String NAME_ID_HEADER_NAME = "name-id";
    public static final String DISPLAY_NAME_HEADER_NAME = "displayname";
    public static final String SCHAC_HOME_HEADER = "schacHomeOrganization";
    public static final String IS_MEMBER_OF_HEADER = "is-member-of";

    private static final Logger LOG = LoggerFactory.getLogger(ShibbolethPreAuthenticatedProcessingFilter.class);

    private final List<Features> featureToggles;
    private final Product product;
    private final Push push;
    private final List<String> superUserTeamNames;
    private final String environment;

    public ShibbolethPreAuthenticatedProcessingFilter(AuthenticationManager authenticationManager,
                                                      List<Features> featureToggles,
                                                      Product product,
                                                      Push push,
                                                      String superUserTeamNamesJoined,
                                                      String environment) {

        super();
        setAuthenticationManager(authenticationManager);
        this.featureToggles = featureToggles;
        this.product = product;
        this.push = push;
        this.superUserTeamNames = parseJoinedTeamNames(superUserTeamNamesJoined, ",");
        this.environment = environment;
    }

    private List<String> parseJoinedTeamNames(String names, String delimiter) {
        return Stream.of(names.split(delimiter))
                .map(String::trim)
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }

    @Override
    protected Object getPreAuthenticatedPrincipal(final HttpServletRequest request) {
        String uid = getHeader(NAME_ID_HEADER_NAME, request);
        String displayName = getHeader(DISPLAY_NAME_HEADER_NAME, request);
        String schacHomeOrganization = getHeader(SCHAC_HOME_HEADER, request);
        String memberOf = getHeader(IS_MEMBER_OF_HEADER, request);

        if (!StringUtils.hasText(uid) || !StringUtils.hasText(displayName)) {
            //this is the contract. See AbstractPreAuthenticatedProcessingFilter#doAuthenticate
            LOG.error("Missing required attribute(s): uid {} displayName {}", uid, displayName);
            return null;
        }
        List<GrantedAuthority> authorityList = createAuthorityList("ROLE_USER", "ROLE_ADMIN");
        if (StringUtils.hasText(memberOf)) {
            List<String> groups = parseJoinedTeamNames(memberOf, ";");
            if (this.superUserTeamNames.stream().anyMatch(groups::contains)) {
                authorityList.add(new SimpleGrantedAuthority("ROLE_".concat(Scope.SYSTEM.name())));
            }
        }
        return new FederatedUser(uid, displayName, schacHomeOrganization,
                authorityList, featureToggles, product, push, environment);
    }

    @Override
    protected Object getPreAuthenticatedCredentials(HttpServletRequest request) {
        return "N/A";
    }

    @SneakyThrows
    private String getHeader(String name, HttpServletRequest request) {
        String header = request.getHeader(name);
            return StringUtils.hasText(header) ?
                    new String(header.getBytes("ISO8859-1"), StandardCharsets.UTF_8) : header;
    }


}
