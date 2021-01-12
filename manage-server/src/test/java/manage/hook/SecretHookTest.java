package manage.hook;

import manage.model.MetaData;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;

public class SecretHookTest {

    private SecretHook subject = new SecretHook();

    @Test
    public void appliesForMetaData() {
        assertEquals(true, subject.appliesForMetaData(new MetaData("oidc10_rp", emptyMap())));
        assertEquals(true, subject.appliesForMetaData(new MetaData("oauth20_rs", emptyMap())));
        assertEquals(false, subject.appliesForMetaData(new MetaData("nope", emptyMap())));
    }

    @Test
    public void postGet() {
        MetaData metaData = subject.postGet(metaData("secret"));
        assertEquals("secret", metaData.metaDataFields().get("secret"));
    }

    @Test
    public void prePut() {
        MetaData metaData = subject.prePut(null, metaData("secret"));
        assertEquals(true, subject.isBCryptEncoded((String) metaData.metaDataFields().get("secret")));

        String secret = (String) metaData.metaDataFields().get("secret");
        metaData = subject.prePut(null, metaData);

        String unchangedSecret = (String) metaData.metaDataFields().get("secret");
        assertEquals(secret, unchangedSecret);
    }

    @Test
    public void prePost() {
        MetaData metaData = subject.prePost(metaData("secret"));
        assertEquals(true, subject.isBCryptEncoded((String) metaData.metaDataFields().get("secret")));

        String secret = (String) metaData.metaDataFields().get("secret");
        metaData = subject.prePost(metaData);

        String unchangedSecret = (String) metaData.metaDataFields().get("secret");
        assertEquals(secret, unchangedSecret);
    }

    @Test
    public void preDelete() {
        MetaData metaData = subject.postGet(metaData("secret"));
        assertEquals("secret", metaData.metaDataFields().get("secret"));
    }

    @Test
    public void isBCryptEncoded() {
        assertEquals(false, subject.isBCryptEncoded("secret"));
    }

    private MetaData metaData(String secret) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> metaDataFields = new HashMap<>();
        data.put("metaDataFields", metaDataFields);

        metaDataFields.put("secret", secret);
        return new MetaData("oidc10_rp", data);
    }
}