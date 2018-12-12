package manage.oidc;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class OpenIdConnectMock implements OpenIdConnect {

    private Map<String, Client> clients = new HashMap<>();

    @Override
    public Optional<Client> getClient(String clientId) {
        Client client = clients.getOrDefault(clientId, defaultClient(clientId));
        return Optional.of(client);
    }

    @Override
    public Client createClient(Client client) {
        return this.updateClient(client);
    }

    @Override
    public Client updateClient(Client client) {
        clients.put(client.getClientId(), client);
        return client;
    }

    @Override
    public void deleteClient(String clientId) {
        clients.remove(clientId);
    }

    private Client defaultClient(String clientId) {
        Client client = new Client();
        client.setGrantTypes(Collections.singleton("authorization_code"));
        client.setClientId(clientId);
        client.setRedirectUris(Collections.singleton("http://redirect"));
        return client;
    }

}
