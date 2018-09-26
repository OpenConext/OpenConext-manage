package manage.format;

import org.springframework.core.io.UrlResource;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class SaveURLResource extends UrlResource {

    private final List<String> allowedProtocols = Arrays.asList("http", "https");

    public SaveURLResource(URL url, boolean dev) {
        super(url);
        String protocol = url.getProtocol();
        if (!dev && !allowedProtocols.contains(protocol)) {
            throw new IllegalArgumentException(String.format("Not allowed protocol %s - allowed protocols are %s", protocol, allowedProtocols));
        }
    }
}
