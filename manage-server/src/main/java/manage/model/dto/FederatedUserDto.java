package manage.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import manage.shibboleth.FederatedUser;
import org.springframework.security.core.GrantedAuthority;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FederatedUserDto {

    private String username;

    private Set<GrantedAuthority> authorities;

    private boolean accountNonExpired;

    private boolean accountNonLocked;

    private boolean credentialsNonExpired;

    private boolean enabled;

    private String uid;

    private String displayName;

    private String schacHomeOrganization;

    private List<Features> featureToggles;

    private Product product;

    private Push push;

    private String name;

    private boolean isAPIUser;

    private boolean isGuest;

    public static FederatedUserDto fromFederatedUser(FederatedUser federatedUser) {
        return null == federatedUser ? null : new FederatedUserDto(
                federatedUser.getUsername(),
                new HashSet<>(federatedUser.getAuthorities()),
                federatedUser.isAccountNonExpired(),
                federatedUser.isAccountNonLocked(),
                federatedUser.isCredentialsNonExpired(),
                federatedUser.isEnabled(),
                federatedUser.getUid(),
                federatedUser.getDisplayName(),
                federatedUser.getSchacHomeOrganization(),
                federatedUser.getFeatureToggles(),
                federatedUser.getProduct(),
                federatedUser.getPush(),
                federatedUser.getName(),
                federatedUser.isAPIUser(),
                federatedUser.isGuest()
        );
    }
}
