package manage.oidc;

import manage.model.MetaData;

public interface OpenIdConnect {

    Client getClient(String clientId);

    Client createClient(Client client);

    Client updateClient(Client client);

    void deleteClient(String clientId);

}
