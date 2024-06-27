package manage.hook;

import manage.TestUtils;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class OidcValidationHookTest implements TestUtils {

    private final OidcValidationHook subject = new OidcValidationHook(new MetaDataAutoConfiguration(
            objectMapper,
            new ClassPathResource("metadata_configuration"),
            new ClassPathResource("metadata_templates")));

    public OidcValidationHookTest() throws IOException {
    }

    @Test
    public void appliesForMetaData() {
        assertTrue(subject.appliesForMetaData(new MetaData(EntityType.RP.getType(), emptyMap())));
        assertTrue(subject.appliesForMetaData(new MetaData(EntityType.SRAM.getType(), Map.of("metaDataFields",
                Map.of("connection_type", "oidc_rp")))));

        assertFalse(subject.appliesForMetaData(new MetaData(EntityType.SP.getType(), emptyMap())));
        assertFalse(subject.appliesForMetaData(new MetaData(EntityType.SRAM.getType(), emptyMap())));
    }

    @Test
    public void prePut() {
        subject.prePut(null, metaData(singletonList("client_credentials"), emptyList()), apiUser());
        subject.prePut(null, metaData(singletonList("authorization_code"), singletonList("https://redirect")), apiUser());
    }

    @Test(expected = ValidationException.class)
    public void prePutValidationException() {
        subject.prePut(null, metaData(singletonList("authorization_code"), emptyList()), apiUser());
    }

    @Test
    public void prePost() {
        subject.prePut(null, metaData(singletonList("client_credentials"), emptyList()), apiUser());
        subject.prePut(null, metaData(singletonList("authorization_code"), singletonList("https://redirect")), apiUser());
    }

    @Test(expected = ValidationException.class)
    public void prePostValidationException() {
        subject.prePut(null, metaData(singletonList("authorization_code"), emptyList()), apiUser());
    }

    @Test(expected = ValidationException.class)
    public void orphanRefreshToken() {
        subject.prePut(null, metaData(singletonList("refresh_token"), singletonList("https://redirect")), apiUser());
    }

    @Test(expected = ValidationException.class)
    public void clientCredentialsWithRedirect() {
        subject.prePut(null, metaData(singletonList("client_credentials"), singletonList("https://redirect")), apiUser());
    }

    @SuppressWarnings("unchecked")
    private MetaData metaData(List<String> grants, List<String> redirectUrls) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> metaDataFields = (Map<String, Object>) data.computeIfAbsent("metaDataFields", key -> new HashMap<String, Object>());
        metaDataFields.put("redirectUrls", redirectUrls);
        metaDataFields.put("grants", grants);
        return new MetaData(EntityType.RP.getType(), data);
    }
}