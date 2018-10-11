package manage.oidc;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Set;

@Getter
@AllArgsConstructor
public class OidcClient {
    private String clientId;
    private String clientSecret;
    private Set<String> redirectUris;
    private String grantType;
    private Set<String> scope;

    public OidcClient(Client client) {
        this.clientId = client.getClientId();
        clientSecret = client.getClientSecret();
        redirectUris = client.getRedirectUris();
        grantType = client.getGrantTypes().isEmpty() ? null : client.getGrantTypes().iterator().next();
        scope = client.getScope();
    }
}
