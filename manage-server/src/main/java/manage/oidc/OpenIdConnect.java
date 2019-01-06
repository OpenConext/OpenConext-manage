package manage.oidc;

import java.util.Optional;

public interface OpenIdConnect {

    Optional<Client> getClient(String clientId);

    Client createClient(Client client);

    Client updateClient(Client client);

    void deleteClient(String clientId);

}
