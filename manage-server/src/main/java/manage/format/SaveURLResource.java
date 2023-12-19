package manage.format;

import org.springframework.core.io.UrlResource;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;

public class SaveURLResource extends UrlResource {

    private static final String USER_AGENT_KEY = "User-Agent";

    private final List<String> allowedProtocols = Arrays.asList("http", "https");

    private final String userAgent;

    public SaveURLResource(URL url, boolean dev, String userAgent) {
        super(url);
        this.userAgent = userAgent;
        String protocol = url.getProtocol();
        if (!dev && !allowedProtocols.contains(protocol)) {
            throw new IllegalArgumentException(String.format("Not allowed protocol %s - allowed protocols are %s", protocol, allowedProtocols));
        }
    }

    @Override
    protected void customizeConnection(URLConnection con) throws IOException {
        super.customizeConnection(con);
        if (null != userAgent && !userAgent.isEmpty()) {
            con.setRequestProperty(USER_AGENT_KEY, userAgent);
        }
    }
}
