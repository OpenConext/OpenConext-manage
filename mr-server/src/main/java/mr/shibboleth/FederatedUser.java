package mr.shibboleth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public class FederatedUser extends User {

    public final String uid;
    public final String displayName;
    public final String schacHomeOrganization;

    public FederatedUser(String uid, String displayName, String schacHomeOrganization, List<GrantedAuthority> authorities) {
        super(uid, "N/A", authorities);
        this.uid = uid;
        this.displayName = displayName;
        this.schacHomeOrganization = schacHomeOrganization;
    }

    public boolean isGuest() {
        return getAuthorities().stream()
            .noneMatch(authority -> authority.getAuthority().equalsIgnoreCase("ROLE_ADMIN"));
    }

}
