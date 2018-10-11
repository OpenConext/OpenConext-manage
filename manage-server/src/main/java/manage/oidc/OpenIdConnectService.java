package manage.oidc;

import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

public class OpenIdConnectService implements OpenIdConnect {

    private String url;
    private RestTemplate restTemplate;

    public OpenIdConnectService(String user, String password, String url) {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(user, password));
//        this.restTemplate.setErrorHandler();
        this.url = url;
    }

    @Override
    public Optional<Client> getClient(String clientId) {
        String url = String.format("%s?clientId=%s", this.url, clientId);
        return Optional.ofNullable(restTemplate.getForEntity(url, Client.class).getBody());
    }

    @Override
    public Client createClient(Client client) {
        ResponseEntity<Client> clientResponseEntity = restTemplate.postForEntity(url, client, Client.class);
        return clientResponseEntity.getBody();
    }

    @Override
    public Client updateClient(Client client) {
        restTemplate.put(url, client);
        return client;
    }

    @Override
    public void deleteClient(String clientId) {
        restTemplate.delete(String.format("%s/clientId=%s", url, clientId));
    }
}
