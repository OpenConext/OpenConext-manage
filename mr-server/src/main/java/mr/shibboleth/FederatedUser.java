package mr.shibboleth;

import lombok.Getter;
import mr.conf.Features;
import mr.conf.Product;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;
import java.util.Map;

@Getter
public class FederatedUser extends User {

    private String uid;
    private String displayName;
    private String schacHomeOrganization;
    private List<Features> featureToggles;
    private Product product;

    public FederatedUser(String uid, String displayName, String schacHomeOrganization, List<GrantedAuthority> authorities, List<Features> featureToggles, Product product) {
        super(uid, "N/A", authorities);
        this.uid = uid;
        this.displayName = displayName;
        this.schacHomeOrganization = schacHomeOrganization;
        this.featureToggles = featureToggles;
        this.product = product;
    }

    public boolean isGuest() {
        return getAuthorities().stream()
            .noneMatch(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }

    public boolean featureAllowed(Features feature) {
        return featureToggles.contains(feature);
    }

}
