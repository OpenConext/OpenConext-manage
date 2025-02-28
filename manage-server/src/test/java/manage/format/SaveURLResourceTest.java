package manage.format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class SaveURLResourceTest {

    @Mock
    URLConnection connection;

    @Test
    public void testNotAllowedProtocol() {
        assertThrows(IllegalArgumentException.class, () ->
                new SaveURLResource(URI.create("file://local").toURL(), false, null));
    }

    @Test
    public void testNotAllowedProtocolInDev() throws MalformedURLException {
        new SaveURLResource(URI.create("file://local").toURL(), true, null);
    }

    @Test
    public void testUseUserAgent() throws IOException {
        SaveURLResource resource = new SaveURLResource(new URL("https://external"), false, "user agent");
        resource.customizeConnection(connection);
        verify(connection, times(1)).setRequestProperty("User-Agent", "user agent");
    }
}