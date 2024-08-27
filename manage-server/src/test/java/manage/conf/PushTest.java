package manage.conf;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PushTest {

    @Test
    public void testUserNamePasswordRemoval() {
        Push push = new Push("https://serviceregistry:secret@engine-api.test2.surfconext.nl/api/connections",
                "name", "oidcUrl", "oidcName", "pdpUrl", "pdpName", true, true);
        assertEquals("https://engine-api.test2.surfconext.nl/api/connections", push.url);
    }

}