package manage.web;

import org.apache.hc.client5.http.auth.AuthCache;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.auth.BasicAuthCache;
import org.apache.hc.client5.http.impl.auth.BasicScheme;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

/**
 * Default HttpClient does not support Preemptive authentication. Spring has added a hook to
 * support this: https://jira.spring.io/browse/SPR-8367
 */
public class PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory extends
        HttpComponentsClientHttpRequestFactory {

    private final HttpContext httpContext;

    public PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory(HttpClient httpClient, String url) throws
            MalformedURLException {
        super(httpClient);
        this.httpContext = this.initHttpContext(url);
    }

    private HttpContext initHttpContext(String url) throws MalformedURLException {
        URL parsedUrl = new URL(url);
        HttpHost targetHost = new HttpHost(parsedUrl.getProtocol(), parsedUrl.getHost(), parsedUrl.getPort());
        AuthCache authCache = new BasicAuthCache();
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);
        BasicHttpContext localContext = new BasicHttpContext();
        localContext.setAttribute(HttpClientContext.AUTH_CACHE, authCache);
        return localContext;
    }

    @Override
    protected HttpContext createHttpContext(HttpMethod httpMethod, URI uri) {
        return this.httpContext;
    }
}
