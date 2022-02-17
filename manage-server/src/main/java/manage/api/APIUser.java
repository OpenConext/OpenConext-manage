package manage.api;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class APIUser implements AbstractUser {

    private String name;
    private String password;
    private List<Scope> scopes;
    private boolean isAPIUser = true;
}
