package manage.hook;

import manage.AbstractIntegrationTest;
import manage.TestUtils;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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

//    private final CertificateDataDuplicationHook certificateDataDuplicationHook = new CertificateDataDuplicationHook(new MetaDataAutoConfiguration(
//        objectMapper,
//        new ClassPathResource("metadata_configuration"),
//        new ClassPathResource("metadata_templates"),
//        ""));
//
//    public CertificateDataDuplicationHookTest() throws IOException {
//    }
//
//    @Test
//    void appliesForMetaData() {
//        Stream.of(EntityType.values())
//            .forEach(entityType -> {
//                boolean appliesForMetaData = certificateDataDuplicationHook.appliesForMetaData(new MetaData(entityType.getType(), Map.of()));
//                boolean expected = entityType.equals(EntityType.IDP) || entityType.equals(EntityType.SP) || entityType.equals(EntityType.SRAM);
//                assertEquals(expected, appliesForMetaData);
//            });
//    }
//
//    @Test
//    void prePutWithDuplicateCerts() {
//        // Test duplicate certData and certData2
//        MetaData metaData = new MetaData(EntityType.IDP.getType(),
//            Map.of("metaDataFields", Map.of(
//                "certData", "CERT_VALUE_1",
//                "certData2", "CERT_VALUE_1"
//            )));
//        assertThrows(ValidationException.class, () -> certificateDataDuplicationHook.prePut(metaData, metaData, apiUser()));
//
//        // Test duplicate certData and certData3
//        MetaData metaData2 = new MetaData(EntityType.SP.getType(),
//            Map.of("metaDataFields", Map.of(
//                "certData", "CERT_VALUE_2",
//                "certData3", "CERT_VALUE_2"
//            )));
//        assertThrows(ValidationException.class, () -> certificateDataDuplicationHook.prePut(metaData2, metaData2, apiUser()));
//
//        // Test all three certificates with duplicates
//        MetaData metaData3 = new MetaData(EntityType.SRAM.getType(),
//            Map.of("metaDataFields", Map.of(
//                "certData", "CERT_VALUE_3",
//                "certData2", "CERT_VALUE_4",
//                "certData3", "CERT_VALUE_3"
//            )));
//        assertThrows(ValidationException.class, () -> certificateDataDuplicationHook.prePut(metaData3, metaData3, apiUser()));
//    }
//
//    @Test
//    void prePutWithUniqueCerts() {
//        // Test with all unique certificates
//        MetaData metaData = new MetaData(EntityType.IDP.getType(),
//            Map.of("metaDataFields", Map.of(
//                "certData", "CERT_VALUE_1",
//                "certData2", "CERT_VALUE_2",
//                "certData3", "CERT_VALUE_3"
//            )));
//        certificateDataDuplicationHook.prePut(metaData, metaData, apiUser());
//
//        // Test with only one certificate
//        MetaData metaData2 = new MetaData(EntityType.SP.getType(),
//            Map.of("metaDataFields", Map.of(
//                "certData", "CERT_VALUE_1"
//            )));
//        certificateDataDuplicationHook.prePut(metaData2, metaData2, apiUser());
//
//        // Test with empty metadata fields
//        MetaData emptyMetaData = new MetaData(EntityType.IDP.getType(),
//            Map.of("metaDataFields", Map.of()));
//        certificateDataDuplicationHook.prePut(emptyMetaData, emptyMetaData, apiUser());
//
//        // Test with null/empty certificate values (should be ignored)
//        MetaData metaData3 = new MetaData(EntityType.IDP.getType(),
//            Map.of("metaDataFields", Map.of(
//                "certData", "CERT_VALUE_1",
//                "certData2", "",
//                "certData3", "CERT_VALUE_3"
//            )));
//        certificateDataDuplicationHook.prePut(metaData3, metaData3, apiUser());
//    }
//
//    @Test
//    void prePostWithDuplicateCerts() {
//        MetaData metaData = new MetaData(EntityType.IDP.getType(),
//            Map.of("metaDataFields", Map.of(
//                "certData", "CERT_VALUE_1",
//                "certData2", "CERT_VALUE_1"
//            )));
//        assertThrows(ValidationException.class, () -> certificateDataDuplicationHook.prePost(metaData, apiUser()));
//    }
//
//    @Test
//    void prePostWithUniqueCerts() {
//        MetaData metaData = new MetaData(EntityType.SP.getType(),
//            Map.of("metaDataFields", Map.of(
//                "certData", "CERT_VALUE_1",
//                "certData2", "CERT_VALUE_2"
//            )));
//        certificateDataDuplicationHook.prePost(metaData, apiUser());
//
//        MetaData emptyMetaData = new MetaData(EntityType.SRAM.getType(),
//            Map.of("metaDataFields", Map.of()));
//        certificateDataDuplicationHook.prePost(emptyMetaData, apiUser());
//    }
}
