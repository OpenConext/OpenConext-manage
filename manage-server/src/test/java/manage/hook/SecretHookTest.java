package manage.hook;

import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;

public class SecretHookTest {

    private final SecretHook subject = new SecretHook();
    private final Pattern bcryptPattern = Pattern.compile("\\A\\$2(a|y|b)?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}");

    @Test
    public void appliesForMetaData() {
        assertTrue(subject.appliesForMetaData(new MetaData("oidc10_rp", emptyMap())));
        assertTrue(subject.appliesForMetaData(new MetaData("oauth20_rs", emptyMap())));
        assertFalse(subject.appliesForMetaData(new MetaData("nope", emptyMap())));
    }

    @Test
    public void postGet() {
        MetaData metaData = subject.postGet(metaData("secret"));
        assertEquals("secret", metaData.metaDataFields().get("secret"));
    }

    @Test
    public void prePut() {
        MetaData metaData = subject.prePut(null, metaData("secret"));
        assertTrue(subject.isBCryptEncoded((String) metaData.metaDataFields().get("secret")));

        String secret = (String) metaData.metaDataFields().get("secret");
        metaData = subject.prePut(null, metaData);

        String unchangedSecret = (String) metaData.metaDataFields().get("secret");
        assertEquals(secret, unchangedSecret);
    }

    @Test
    public void prePost() {
        MetaData metaData = subject.prePost(metaData("secret"));
        String encodedPassword = (String) metaData.metaDataFields().get("secret");

        assertTrue(subject.isBCryptEncoded(encodedPassword));
        assertEquals(10, this.getStrength(encodedPassword));

        metaData = subject.prePost(metaData);

        String unchangedSecret = (String) metaData.metaDataFields().get("secret");
        assertEquals(encodedPassword, unchangedSecret);
    }

    @Test
    public void preDelete() {
        MetaData metaData = subject.postGet(metaData("secret"));
        assertEquals("secret", metaData.metaDataFields().get("secret"));
    }

    @Test
    public void isBCryptEncoded() {
        assertFalse(subject.isBCryptEncoded("secret"));
    }

    @Test
    public void resourceServerWeakerStrength() {
        MetaData metaData = subject.prePost(metaData("secret", EntityType.RS));
        String encodedPassword = (String) metaData.metaDataFields().get("secret");

        assertTrue(subject.isBCryptEncoded(encodedPassword));
        assertEquals(5, this.getStrength(encodedPassword));
    }

    private int getStrength(String encodedPassword) {
        Matcher matcher = this.bcryptPattern.matcher(encodedPassword);
        boolean matches = matcher.matches();

        assertTrue(matches);

        return Integer.parseInt(matcher.group(2));
    }

    private MetaData metaData(String secret) {
        return this.metaData(secret, EntityType.RP);
    }

    private MetaData metaData(String secret, EntityType entityType) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> metaDataFields = new HashMap<>();
        data.put("metaDataFields", metaDataFields);

        metaDataFields.put("secret", secret);
        return new MetaData(entityType.getType(), data);
    }
}