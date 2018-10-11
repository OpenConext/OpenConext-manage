package manage.oidc;

import java.util.Collections;
import java.util.Optional;

public class OpenIdConnectMock implements OpenIdConnect {

    @Override
    public Optional<Client> getClient(String clientId) {
        Client client = new Client();
        client.setGrantTypes(Collections.singleton("authorization_code"));
        client.setClientId(clientId);
        client.setRedirectUris(Collections.singleton("http://redirect"));
        return Optional.of(client);
    }

    @Override
    public Client createClient(Client client) {
        return client;
    }

    @Override
    public Client updateClient(Client client) {
        return client;
    }

    @Override
    public void deleteClient(String clientId) {

    }
}
