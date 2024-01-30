package manage.api;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
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
    private ImpersonatedUser impersonatedUser;

    public APIUser(String name, List<Scope> scopes) {
        this.name = name;
        this.scopes = scopes;
    }

    public APIUser(APIUser apiUser) {
        this.name = apiUser.getName();
        this.scopes = apiUser.getScopes();
        this.environment = apiUser.getEnvironment();
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
