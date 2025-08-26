package manage.hook;

import manage.TestUtils;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdentityProviderBrinCodeHookTest implements TestUtils {

    private final IdentityProviderBrinCodeHook identityProviderBrinCodeHook = new IdentityProviderBrinCodeHook(new MetaDataAutoConfiguration(
        objectMapper,
        new ClassPathResource("metadata_configuration"),
        new ClassPathResource("metadata_templates")));

    public IdentityProviderBrinCodeHookTest() throws IOException {
    }

    @Test
    void appliesForMetaData() {
        Stream.of(EntityType.values())
            .forEach(entityType -> {
                boolean appliesForMetaData = identityProviderBrinCodeHook.appliesForMetaData(new MetaData(entityType.getType(), Map.of()));
                assertEquals(entityType.equals(EntityType.IDP), appliesForMetaData);
            });
    }

    @Test
    void prePut() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "coin:institution_brin", "QW12"
            )));
        assertThrows(ValidationException.class, () -> identityProviderBrinCodeHook.prePut(metaData, metaData, apiUser()));
        MetaData newMetaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "coin:institution_brin_schac_home", "example.com"
            )));
        assertThrows(ValidationException.class, () -> identityProviderBrinCodeHook.prePut(newMetaData, newMetaData, apiUser()));

        MetaData emptyMetaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of()));
        identityProviderBrinCodeHook.prePut(emptyMetaData, emptyMetaData, apiUser());

        MetaData correctMetaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "coin:institution_brin", "QW12",
                "coin:institution_brin_schac_home", "example.com"
            )));
        identityProviderBrinCodeHook.prePut(correctMetaData, correctMetaData, apiUser());
    }

    @Test
    void prePost() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "coin:institution_brin", "QW12"
            )));
        assertThrows(ValidationException.class, () -> identityProviderBrinCodeHook.prePost(metaData, apiUser()));
        MetaData newMetaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "coin:institution_brin_schac_home", "example.com"
            )));
        assertThrows(ValidationException.class, () -> identityProviderBrinCodeHook.prePost(newMetaData, apiUser()));

        MetaData emptyMetaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of()));
        identityProviderBrinCodeHook.prePost(emptyMetaData, apiUser());

        MetaData correctMetaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "coin:institution_brin", "QW12",
                "coin:institution_brin_schac_home", "example.com"
            )));
        identityProviderBrinCodeHook.prePost(correctMetaData, apiUser());
    }
}
