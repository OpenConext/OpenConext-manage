package manage.shibboleth;

import lombok.Getter;
import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class FederatedUser extends User {

    private String uid;
    private String displayName;
    private String schacHomeOrganization;
    private List<Features> featureToggles;
    private Product product;
    private Push push;

    public FederatedUser(String uid, String displayName, String schacHomeOrganization, List<GrantedAuthority> authorities, List<Features> featureToggles, Product product, Push push) {
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

}
