package manage.hook;

import manage.AbstractIntegrationTest;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DuplicateEntityReferenceHookTest extends AbstractIntegrationTest {

    private DuplicateEntityReferenceHook duplicateEntityReferenceHook;

    @BeforeEach
    public void before() throws Exception {
        super.before();
        duplicateEntityReferenceHook = new DuplicateEntityReferenceHook(metaDataAutoConfiguration);
    }

    @ParameterizedTest
    @MethodSource("entityTypeProvider")
    void appliesForMetaDataTest(EntityType entityType, boolean expected) {
        boolean result = duplicateEntityReferenceHook.appliesForMetaData(
            new MetaData(entityType.getType(), Map.of())
        );
        assertEquals(expected, result,
            "Entity type " + entityType + " should " + (expected ? "" : "not ") + "apply");
    }

    private static Stream<Arguments> entityTypeProvider() {
        return Stream.of(
            Arguments.of(EntityType.IDP, true),
            Arguments.of(EntityType.SP, true),
            Arguments.of(EntityType.RP, true),
            Arguments.of(EntityType.STT, true),
            Arguments.of(EntityType.SRAM, true),
            Arguments.of(EntityType.PDP, true),
            Arguments.of(EntityType.RS, false),
            Arguments.of(EntityType.PROV, false),
            Arguments.of(EntityType.ORG, false),
            Arguments.of(EntityType.SFO, false),
            Arguments.of(EntityType.STEPUP, false)
        );
    }

    @Test
    void prePostTest_DuplicateStepupEntities() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("stepupEntities", List.of(
                Map.of("name", "https://sp1", "level", "loa2"),
                Map.of("name", "https://sp1", "level", "loa3")
            )));
        assertThrows(ValidationException.class, () -> duplicateEntityReferenceHook.prePost(metaData, apiUser()));
    }

    @Test
    void prePutTest_DuplicateStepupEntities() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("stepupEntities", List.of(
                Map.of("name", "https://sp1", "level", "loa2"),
                Map.of("name", "https://sp1", "level", "loa3")
            )));
        assertThrows(ValidationException.class, () -> duplicateEntityReferenceHook.prePut(metaData, metaData, apiUser()));
    }

    @Test
    void prePostTest_NoDuplicates() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("stepupEntities", List.of(
                Map.of("name", "https://sp1", "level", "loa2"),
                Map.of("name", "https://sp2", "level", "loa3")
            )));
        MetaData result = duplicateEntityReferenceHook.prePost(metaData, apiUser());
        assertEquals(metaData, result);
    }

    @Test
    void prePostTest_AttributeAbsent() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(), Map.of("entityid", "https://idp"));
        MetaData result = duplicateEntityReferenceHook.prePost(metaData, apiUser());
        assertEquals(metaData, result);
    }

    @Test
    void prePostTest_DuplicateAllowedEntities() {
        MetaData metaData = new MetaData(EntityType.SP.getType(),
            Map.of("allowedEntities", List.of(
                Map.of("name", "https://idp1"),
                Map.of("name", "https://idp1")
            )));
        assertThrows(ValidationException.class, () -> duplicateEntityReferenceHook.prePost(metaData, apiUser()));
    }

    @Test
    void prePostTest_DuplicateDisableConsent() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("disableConsent", List.of(
                Map.of("name", "https://sp1", "type", "no-consent"),
                Map.of("name", "https://sp1", "type", "explicit-no-consent")
            )));
        assertThrows(ValidationException.class, () -> duplicateEntityReferenceHook.prePost(metaData, apiUser()));
    }

    @Test
    void prePostTest_DuplicateMfaEntities() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("mfaEntities", List.of(
                Map.of("name", "https://sp1", "level", "loa2"),
                Map.of("name", "https://sp1", "level", "loa2")
            )));
        assertThrows(ValidationException.class, () -> duplicateEntityReferenceHook.prePost(metaData, apiUser()));
    }

    @Test
    void prePostTest_DuplicateAllowedResourceServers() {
        MetaData metaData = new MetaData(EntityType.RP.getType(),
            Map.of("allowedResourceServers", List.of(
                Map.of("name", "https://rs1"),
                Map.of("name", "https://rs1")
            )));
        assertThrows(ValidationException.class, () -> duplicateEntityReferenceHook.prePost(metaData, apiUser()));
    }

    @Test
    void prePostTest_DuplicateServiceProviderIds() {
        MetaData metaData = new MetaData(EntityType.PDP.getType(),
            Map.of("serviceProviderIds", List.of(
                Map.of("name", "https://sp1"),
                Map.of("name", "https://sp1")
            )));
        assertThrows(ValidationException.class, () -> duplicateEntityReferenceHook.prePost(metaData, apiUser()));
    }

    @Test
    void prePostTest_DuplicateIdentityProviderIds() {
        MetaData metaData = new MetaData(EntityType.PDP.getType(),
            Map.of("identityProviderIds", List.of(
                Map.of("name", "https://idp1"),
                Map.of("name", "https://idp1")
            )));
        assertThrows(ValidationException.class, () -> duplicateEntityReferenceHook.prePost(metaData, apiUser()));
    }

    @Test
    void prePostTest_MixedValidAndDuplicateEntries() {
        MetaData metaData = new MetaData(EntityType.SP.getType(),
            Map.of("allowedEntities", List.of(
                Map.of("name", "https://idp1"),
                Map.of("name", "https://idp2"),
                Map.of("name", "https://idp1")
            )));
        ValidationException exception = assertThrows(ValidationException.class,
            () -> duplicateEntityReferenceHook.prePost(metaData, apiUser()));
        assertEquals(true, exception.getMessage().contains("https://idp1"));
    }

}
