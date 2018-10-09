package manage.oidc;

import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.RestTemplate;

public class OpenIdConnectService implements OpenIdConnect {

    private String url;
    private RestTemplate restTemplate;

    public OpenIdConnectService(String user, String password, String url) {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(user, password));
        this.url = url;
    }


    @Override
    public Client getClient(String clientId) {
        return restTemplate.getForEntity(String.format("%s/clientId=%s", url, clientId), Client.class).getBody();
    }

    @Override
    public Client createClient(Client client) {
        return restTemplate.postForEntity(url, client, Client.class).getBody();
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
