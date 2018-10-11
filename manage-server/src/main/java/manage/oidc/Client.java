package manage.oidc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class Client {

    private String clientId;
    private String clientSecret;
    private Set<String> redirectUris;
    private String clientName;
    private String tokenEndpointAuthMethod = "SECRET_BASIC";
    private Set<String> scope = Collections.singleton("openid");
    private Set<String> grantTypes = Collections.singleton("authorization_code");
    private Set<String> responseTypes = Collections.singleton("code");
    private String subjectType = "PUBLIC";
    private int accessTokenValiditySeconds = 1440;
    private int idTokenValiditySeconds = 60;
    private Set<String> authorizedGrantTypes = grantTypes;
    private Set<String> registeredRedirectUri;
    private boolean secretRequired = true;
    private boolean scoped = true;
    private boolean requireAuthTime = true;
    private boolean clearAccessTokensOnRefresh = true;

}
