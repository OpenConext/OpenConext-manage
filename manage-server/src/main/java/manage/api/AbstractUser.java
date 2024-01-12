package manage.api;

import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public interface AbstractUser {
    String getName();

    boolean isAPIUser();

    Collection<GrantedAuthority> getAuthorities();

    boolean isSuperUser();

    String getEnvironment() ;

    default boolean isProductionEnvironment() {
        return getEnvironment().equals("prod");
    }
}
