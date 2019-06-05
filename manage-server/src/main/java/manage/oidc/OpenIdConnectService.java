package manage.oidc;

import manage.control.MetaDataController;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Optional;

public class OpenIdConnectService implements OpenIdConnect {

    private static final Logger LOG = LoggerFactory.getLogger(OpenIdConnectService.class);

    private String url;
    private RestTemplate restTemplate;

    public OpenIdConnectService(String user, String password, String url) {
        this.restTemplate = new RestTemplate();
        this.restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(user, password));
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler(){
            @Override
            public void handleError(ClientHttpResponse response) throws IOException {
                String body = IOUtils.toString(response.getBody(), Charset.defaultCharset());
                LOG.error(String.format("Error from OIDC: %s", body));
                super.handleError(response);
            }
        });
        this.url = url;
    }

    @Override
    public Optional<Client> getClient(String clientId) {
        String url = String.format("%s?clientId=%s", this.url, clientId);
        try {
            return Optional.ofNullable(restTemplate.getForEntity(url, Client.class).getBody());
        } catch (HttpClientErrorException e) {
            LOG.error("Error in retrieving client " + clientId + " from OIDC", e);
            return Optional.empty();
        }
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
        try {
            restTemplate.delete(String.format("%s?clientId=%s", url, clientId));
        } catch (HttpClientErrorException e) {
            LOG.error("Error in deleting client " + clientId + " from OIDC", e);
        }

    }
}
