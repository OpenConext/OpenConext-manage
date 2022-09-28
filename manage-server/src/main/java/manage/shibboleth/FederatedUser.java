package manage.shibboleth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import manage.api.AbstractUser;
import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.io.Serializable;
import java.util.List;

@Getter
public class FederatedUser extends User implements Serializable, AbstractUser {

    private final String uid;
    private final String displayName;
    private final String schacHomeOrganization;
    private final List<Features> featureToggles;
    private final Product product;
    private final Push push;

    public FederatedUser(String uid, String displayName, String schacHomeOrganization, List<GrantedAuthority>
            authorities, List<Features> featureToggles, Product product, Push push) {
        super(uid, "N/A", authorities);
        this.uid = uid;
        this.displayName = displayName;
        this.schacHomeOrganization = schacHomeOrganization;
        this.featureToggles = featureToggles;
        this.product = product;
        this.push = push;
    }

    public boolean isGuest() {
        return getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
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
}
