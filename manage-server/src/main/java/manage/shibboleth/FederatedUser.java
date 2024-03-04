package manage.shibboleth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import manage.api.AbstractUser;
import manage.api.Scope;
import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public class FederatedUser extends User implements Serializable, AbstractUser {

    private final String uid;
    private final String displayName;
    private final String schacHomeOrganization;
    private final List<Features> featureToggles;
    private final Product product;
    private final Push push;
    private final String environment;
    private List<Scope> scopes;

    public FederatedUser(List<Scope> scopes) {
        this("uid", "displayName", "schacHomeOrganization", Collections.emptyList(), Collections.emptyList(), null, null, "environment");
        this.scopes = scopes;
    }

    public FederatedUser(String uid, String displayName, String schacHomeOrganization, List<GrantedAuthority>
            authorities, List<Features> featureToggles, Product product, Push push, String environment) {
        super(uid, "N/A", authorities);
        this.uid = uid;
        this.displayName = displayName;
        this.schacHomeOrganization = schacHomeOrganization;
        this.featureToggles = featureToggles;
        this.product = product;
        this.push = push;
        this.environment = environment;
        this.scopes = authorities.stream()
                .map(authority -> Scope.valueOf(authority.getAuthority().replaceAll("ROLE_", "")))
                .collect(Collectors.toList());
    }

    public boolean featureAllowed(Features feature) {
        return featureToggles.contains(feature);
    }

    @Override
    public String getName() {
        return this.uid;
    }

    @Override
    public boolean isAPIUser() {
        return false;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return super.getPassword();
    }

    @Override
    public boolean isAllowed(Scope... scopes) {
        return this.scopes.containsAll(Arrays.asList(scopes));
    }

    @Override
    public boolean isSystemUser() {
        return this.scopes.contains(Scope.SYSTEM);
    }
}
