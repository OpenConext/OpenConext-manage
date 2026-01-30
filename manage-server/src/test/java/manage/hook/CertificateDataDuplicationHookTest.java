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

import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CertificateDataDuplicationHookTest extends AbstractIntegrationTest {

    private CertificateDataDuplicationHook certificateDataDuplicationHook;

    @BeforeEach
    public void before() throws Exception {
        super.before();
        certificateDataDuplicationHook = new CertificateDataDuplicationHook(metaDataAutoConfiguration);
    }

    @ParameterizedTest
    @MethodSource("entityTypeProvider")
    void appliesForMetaDataTest(EntityType entityType, boolean expected) {
        boolean result = certificateDataDuplicationHook.appliesForMetaData(
            new MetaData(entityType.getType(), Map.of())
        );
        assertEquals(expected, result, 
            "Entity type " + entityType + " should " + (expected ? "" : "not ") + "apply");
    }

    private static Stream<Arguments> entityTypeProvider() {
        return Stream.of(
            Arguments.of(EntityType.IDP, true),
            Arguments.of(EntityType.SP, true),
            Arguments.of(EntityType.SRAM, true),
            Arguments.of(EntityType.RP, false),
            Arguments.of(EntityType.RS, false),
            Arguments.of(EntityType.STT, false),
            Arguments.of(EntityType.PROV, false),
            Arguments.of(EntityType.PDP, false),
            Arguments.of(EntityType.ORG, false)
        );
    }

    @Test
    void prePutTest() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "certData", "CERT_VALUE_1",
                "certData2", "CERT_VALUE_2",
                "certData3", "CERT_VALUE_3"
            )));
        MetaData result = certificateDataDuplicationHook.prePut(metaData, metaData, apiUser());
        assertEquals(metaData, result);
    }

    @Test
    void prePutTest_AllSameValues() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "certData", "CERT_VALUE_1",
                "certData2", "CERT_VALUE_1",
                "certData3", "CERT_VALUE_1"
            )));
        assertThrows(ValidationException.class, () -> certificateDataDuplicationHook.prePut(metaData, metaData, apiUser()));
    }

    @Test
    void prePutTest_OneMissing() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "certData", "CERT_VALUE_1",
                "certData3", "CERT_VALUE_3"
            )));
        MetaData result = certificateDataDuplicationHook.prePut(metaData, metaData, apiUser());
        assertEquals(metaData, result);
    }

    @Test
    void prePutTest_ValueEmptyString() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "certData", "CERT_VALUE_1",
                "certData2", "",
                "certData3", ""
            )));
        MetaData result = certificateDataDuplicationHook.prePut(metaData, metaData, apiUser());
        assertEquals(metaData, result);
    }

    @Test
    void prePostTest() {
        MetaData metaData = new MetaData(EntityType.SP.getType(),
            Map.of("metaDataFields", Map.of(
                "certData", "CERT_VALUE_1",
                "certData2", "CERT_VALUE_2",
                "certData3", "CERT_VALUE_3"
            )));
        MetaData result = certificateDataDuplicationHook.prePost(metaData, apiUser());
        assertEquals(metaData, result);
    }

    @Test
    void prePostTest_AllSameValues() {
        MetaData metaData = new MetaData(EntityType.SRAM.getType(),
            Map.of("metaDataFields", Map.of(
                "certData", "CERT_VALUE_1",
                "certData2", "CERT_VALUE_1",
                "certData3", "CERT_VALUE_1"
            )));
        assertThrows(ValidationException.class, () -> certificateDataDuplicationHook.prePost(metaData, apiUser()));
    }

    @Test
    void prePostTest_OneMissing() {
        MetaData metaData = new MetaData(EntityType.IDP.getType(),
            Map.of("metaDataFields", Map.of(
                "certData", "CERT_VALUE_1",
                "certData3", "CERT_VALUE_3"
            )));
        MetaData result = certificateDataDuplicationHook.prePost(metaData, apiUser());
        assertEquals(metaData, result);
    }

    @Test
    void prePostTest_ValueEmptyString() {
        MetaData metaData = new MetaData(EntityType.SP.getType(),
            Map.of("metaDataFields", Map.of(
                "certData", "CERT_VALUE_1",
                "certData2", "",
                "certData3", ""
            )));
        MetaData result = certificateDataDuplicationHook.prePost(metaData, apiUser());
        assertEquals(metaData, result);
    }
}
