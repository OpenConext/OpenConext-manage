package manage.oidc;

import manage.model.MetaData;

import java.util.Collections;
import java.util.Set;

public class OpenIdConnectMock implements OpenIdConnect {
    @Override
    public Client getClient(String clientId) {
        Client client = new Client();
        client.setGrantTypes(Collections.singleton("authorization_code"));
        client.setClientId(clientId);
        client.setRedirectUris(Collections.singleton("http://redirect"));
        return client;
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
