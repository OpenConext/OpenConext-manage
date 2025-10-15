package manage.control;

import lombok.SneakyThrows;
import manage.web.HttpHostProvider;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.core5.http.HttpHost;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Optional;

public class RestTemplateIdiom {
    private RestTemplateIdiom() {
    }

    @SneakyThrows
    public static RestTemplate buildRestTemplate(String uri, String userName, String password) {

        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
            .setConnectionManager(new PoolingHttpClientConnectionManager());

        if (StringUtils.hasText(uri)) {
            Optional<HttpHost> optionalHttpHost = HttpHostProvider.resolveHttpHost(URI.create(uri).toURL());
            optionalHttpHost.ifPresent(httpHost -> httpClientBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(httpHost)));
        }

        CloseableHttpClient httpClient = httpClientBuilder.build();

        HttpComponentsClientHttpRequestFactory requestFactory =
            new HttpComponentsClientHttpRequestFactory(httpClient);

        RestTemplateBuilder builder = new RestTemplateBuilder();
        return builder
            .requestFactory(() -> requestFactory)
            .additionalInterceptors(List.of(
                new BasicAuthenticationInterceptor(userName, password),
                new CookieRemoveInterceptor()))
            .build();
    }

    private static class CookieRemoveInterceptor implements ClientHttpRequestInterceptor {

        @Override
        public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
            request.getHeaders().remove(HttpHeaders.COOKIE);
            return execution.execute(request, body);
        }
    }
}

