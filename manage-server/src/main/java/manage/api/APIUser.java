package manage.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
public class APIUser implements AbstractUser {

    private String name;
    private String password;
    private List<Scope> scopes;
    private String environment;
    private boolean isAPIUser = true;

    public APIUser(String name, List<Scope> scopes) {
        this.name = name;
        this.scopes = scopes;
    }

    @Override
    public Collection<GrantedAuthority> getAuthorities() {
        return scopes.stream()
                .map(scope -> new SimpleGrantedAuthority("ROLE_".concat(scope.name())))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isSuperUser() {
        return false;
    }
}
