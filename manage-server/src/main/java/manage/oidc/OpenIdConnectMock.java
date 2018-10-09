package manage.oidc;

import manage.model.MetaData;

public class OpenIdConnectMock implements OpenIdConnect {
    @Override
    public Client getClient(String clientId) {
        return new Client();
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
