package manage.hook;

import manage.AbstractIntegrationTest;
import manage.TestUtils;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;

public class SecretHookTest extends AbstractIntegrationTest implements TestUtils {

    private SecretHook subject;
    private final Pattern bcryptPattern = Pattern.compile("\\A\\$2(a|y|b)?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}");

    @Before
    public void before() throws Exception {
        super.before();
        subject = new SecretHook(metaDataAutoConfiguration);
    }

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
        MetaData metaData = subject.prePut(null, metaData("secret1234567890"), apiUser());
        assertTrue(subject.isBCryptEncoded((String) metaData.metaDataFields().get("secret")));

        String secret = (String) metaData.metaDataFields().get("secret");
        metaData = subject.prePut(null, metaData, apiUser());

        String unchangedSecret = (String) metaData.metaDataFields().get("secret");
        assertEquals(secret, unchangedSecret);
    }

    @Test
    public void prePost() {
        MetaData metaData = subject.prePost(metaData("secret1234567890"), apiUser());
        String encodedPassword = (String) metaData.metaDataFields().get("secret");

        assertTrue(subject.isBCryptEncoded(encodedPassword));
        assertEquals(5, this.getStrength(encodedPassword));

        metaData = subject.prePost(metaData, apiUser());

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
        MetaData metaData = subject.prePost(metaData("secret1234567890", EntityType.RS), apiUser());
        String encodedPassword = (String) metaData.metaDataFields().get("secret");

        assertTrue(subject.isBCryptEncoded(encodedPassword));
        assertEquals(5, this.getStrength(encodedPassword));
    }

    @Test(expected = ValidationException.class)
    public void minimalLength() {
        subject.prePost(metaData("secret", EntityType.RS), apiUser());
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