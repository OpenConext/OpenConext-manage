package manage.format;

import org.junit.Test;

import java.net.MalformedURLException;
import java.net.URL;

public class SaveURLResourceTest {

    @Test(expected = IllegalArgumentException.class)
    public void testNotAllowedProtocol() throws MalformedURLException {
        SaveURLResource resource = new SaveURLResource(new URL("file://local"), false);
    }

    @Test
    public void testNotAllowedProtocolInDev() throws MalformedURLException {
        new SaveURLResource(new URL("file://local"), true);
    }
}