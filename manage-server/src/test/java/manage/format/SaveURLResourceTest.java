package manage.format;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SaveURLResourceTest {

    @Mock
    URLConnection connection;

    @Test(expected = IllegalArgumentException.class)
    public void testNotAllowedProtocol() throws MalformedURLException {
        new SaveURLResource(new URL("file://local"), false, null);
    }

    @Test
    public void testNotAllowedProtocolInDev() throws MalformedURLException {
        new SaveURLResource(new URL("file://local"), true, null);
    }

    @Test
    public void testUseUserAgent() throws IOException {
        SaveURLResource resource = new SaveURLResource(new URL("https://external"), false, "user agent");
        resource.customizeConnection(connection);
        verify(connection, times(1)).setRequestProperty("User-Agent", "user agent");
    }
}