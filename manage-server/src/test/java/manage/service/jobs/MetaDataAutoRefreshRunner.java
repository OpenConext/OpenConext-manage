package manage.service.jobs;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.fasterxml.jackson.core.JsonProcessingException;
import manage.conf.Features;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.Revision;
import manage.service.FeatureService;
import manage.service.ImporterService;
import manage.service.MetaDataService;
import manage.util.LogUtils;
import manage.util.MemoryAppender;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class MetadataAutoRefreshRunnerTest {

    @Mock
    MetaDataService metaDataService;

    @Mock
    ImporterService importerService;

    @Mock
    FeatureService featureService;

    @Mock
    MetaDataAutoConfiguration metaDataAutoConfiguration;

    MetadataAutoRefreshRunner autoRefreshRunner;

    @Captor
    ArgumentCaptor<MetaData> metaDataCaptor;

    MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        // Configure auto refresh fields for mocking the configuration in .schema.json files
        Map<String, Object> schemaProperties = new HashMap<>();
        Map<String, Object> autoRefresh = new HashMap<>();
        schemaProperties.put(MetadataAutoRefreshRunner.PROPERTIES_KEY, autoRefresh);
        Map<String, Object> autoRefreshConfig = new HashMap<>();
        autoRefresh.put(MetadataAutoRefreshRunner.AUTO_REFRESH_KEY, autoRefreshConfig);
        Map<String, Object> autoRefreshProperties = new HashMap<>();
        autoRefreshConfig.put(MetadataAutoRefreshRunner.PROPERTIES_KEY, autoRefreshProperties);
        Map<String, Object> fields = new HashMap<>();
        autoRefreshProperties.put(MetadataAutoRefreshRunner.FIELDS_KEY, fields);
        Map<String, Object> configurableFields = new HashMap<>();
        fields.put(MetadataAutoRefreshRunner.PROPERTIES_KEY, configurableFields);
        configurableFields.put("field1", null);
        configurableFields.put("field2", null);
        configurableFields.put("field3", null);

        // Configure mock data returned from importerService.importXMLUrl()
        Map<String, Object> retrievedMetadata = new HashMap<>();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("field1", "value");
        metadata.put("field2", "value");
        metadata.put("field3", "value");
        metadata.put("field4", "value");
        retrievedMetadata.put(MetadataAutoRefreshRunner.METADATA_FIELDS_KEY, metadata);

        when(featureService.isFeatureEnabled(Features.AUTO_REFRESH)).thenReturn(true);
        lenient().when(metaDataAutoConfiguration.schemaRepresentation(EntityType.SP)).thenReturn(schemaProperties);
        lenient().when(metaDataAutoConfiguration.schemaRepresentation(EntityType.IDP)).thenReturn(schemaProperties);
        lenient().when(importerService.importXMLUrl(eq(EntityType.SP), any())).thenReturn(retrievedMetadata);
        lenient().when(importerService.importXMLUrl(eq(EntityType.IDP), any())).thenReturn(retrievedMetadata);

        Logger logger = (Logger) LoggerFactory.getLogger(MetadataAutoRefreshRunner.class);
        memoryAppender = LogUtils.configureLogger(logger);
        memoryAppender.start();

        autoRefreshRunner = new MetadataAutoRefreshRunner(metaDataService, importerService, metaDataAutoConfiguration, featureService, true);
    }

    @Test
    void autoRefreshNotEnabled() {
        when(featureService.isFeatureEnabled(Features.AUTO_REFRESH)).thenReturn(false);

        autoRefreshRunner.run();

        verifyNoInteractions(metaDataService);
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    @Test
    void updateServiceProviderAllowAll() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.SP, "entityId", "metadataUrl", true, true, new HashMap<>());
        current.metaDataFields().put("field1", "old");

        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());

        assertEquals("old", current.metaDataFields().get("field1"));

        autoRefreshRunner.run();

        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.SP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.SP), any());
        verify(metaDataService, times(1)).doPut(metaDataCaptor.capture(), anyString(), anyBoolean());

        MetaData result = metaDataCaptor.getValue();
        assertEquals("value", result.metaDataFields().get("field1"));
        assertEquals("value", result.metaDataFields().get("field2"));
        assertEquals("value", result.metaDataFields().get("field3"));
        assertFalse(result.metaDataFields().containsKey("field4"));
        assertEquals(MetadataAutoRefreshRunner.AUTO_REFRESH_REVISION_NOTE, result.getData().get(Revision.REVISION_KEY));
    }

    @Test
    void updateServiceProviderAllowSubset() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.SP, "entityId", "metadataUrl", true,
                false, Collections.singletonMap("field1", true));
        current.metaDataFields().put("field1", "old");
        current.metaDataFields().put("field2", "old");

        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());

        assertEquals("old", current.metaDataFields().get("field1"));
        assertEquals("old", current.metaDataFields().get("field2"));

        autoRefreshRunner.run();

        verify(metaDataAutoConfiguration, times(0)).schemaRepresentation(EntityType.SP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.SP), any());
        verify(metaDataService, times(1)).doPut(metaDataCaptor.capture(), anyString(), anyBoolean());

        MetaData result = metaDataCaptor.getValue();
        assertEquals("value", result.metaDataFields().get("field1"));
        assertEquals("old", result.metaDataFields().get("field2"));
        assertFalse(result.metaDataFields().containsKey("field3"));
        assertFalse(result.metaDataFields().containsKey("field4"));
        assertEquals(MetadataAutoRefreshRunner.AUTO_REFRESH_REVISION_NOTE, result.getData().get(Revision.REVISION_KEY));
    }

    @Test
    void updateServiceProviderNoMatchingSubset() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.SP, "entityId", "metadataUrl", true,
                false, Collections.singletonMap("field5", false));
        current.metaDataFields().put("field5", "old");
        current.metaDataFields().put("field6", "old");

        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataAutoConfiguration, times(0)).schemaRepresentation(EntityType.SP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.SP), any());
        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
    }

    @Test
    void updateServiceProviderRemoveFieldIfRemovedFromMetadata() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.SP, "entityId", "metadataUrl", true,
                false, Collections.singletonMap("field5", true));
        current.metaDataFields().put("field5", "old");
        current.metaDataFields().put("field6", "old");

        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataAutoConfiguration, times(0)).schemaRepresentation(EntityType.SP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.SP), any());
        verify(metaDataService, times(1)).doPut(metaDataCaptor.capture(), anyString(), anyBoolean());

        MetaData result = metaDataCaptor.getValue();
        assertFalse(result.metaDataFields().containsKey("field5"));
        assertEquals(MetadataAutoRefreshRunner.AUTO_REFRESH_REVISION_NOTE, result.getData().get(Revision.REVISION_KEY));
    }

    @Test
    void updateServiceProviderDoNotAllowSubsetWithoutFields() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.SP, "entityId", "metadataUrl", true, false, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    @Test
    void updateServiceProviderNoChanges() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.SP, "entityId", "metadataUrl", true, true, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.doPut(any(), anyString(), anyBoolean()))
                .thenThrow(new ValidationException(null, "No data is changed", ""));

        autoRefreshRunner.run();

        verify(metaDataService, times(1)).doPut(any(), anyString(), anyBoolean());
        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.SP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.SP), any());
    }

    @Test
    void updateServiceProviderInvalidMetadata() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.SP, "entityId", "metadataUrl", true, true, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());
        when(importerService.importXMLUrl(eq(EntityType.SP), any())).thenReturn(Collections.singletonMap("errors", "invalid metadata"));

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.SP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.SP), any());
    }

    @Test
    void updateServiceProviderValidationError() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.SP, "entityId", "metadataUrl", true, true, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.doPut(any(), anyString(), anyBoolean()))
                .thenThrow(new ValidationException(null, "some error occurred", ""));

        autoRefreshRunner.run();

        verify(metaDataService, times(1)).doPut(any(), anyString(), anyBoolean());
        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.SP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.SP), any());
        assertTrue(memoryAppender.contains("Failed to save changes for saml20_sp entityId: #: some error occurred", Level.INFO));
    }

    @Test
    void updateServiceProviderJsonProcessingError() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.SP, "entityId", "metadataUrl", true, true, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.doPut(any(), anyString(), anyBoolean()))
                .thenThrow(new MockJsonProcessingException("some error occurred"));

        autoRefreshRunner.run();

        verify(metaDataService, times(1)).doPut(any(), anyString(), anyBoolean());
        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.SP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.SP), any());
        assertTrue(memoryAppender.contains("Failed to save changes for saml20_sp entityId: some error occurred", Level.INFO));
    }

    @Test
    void updateServiceProviderNoAutoRefreshSettings() throws JsonProcessingException {
        MetaData metaData = buildMetadata(EntityType.SP, "entityId", "metadataUrl", false, false, new HashMap<>());
        metaData.getData().put(MetadataAutoRefreshRunner.AUTO_REFRESH_KEY, null);
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(metaData));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    @Test
    void updateServiceProviderAutoRefreshDisabled() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.SP, "entityId", "metadataUrl", false, false, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    @Test
    void updateServiceProviderNoMetadataUrl() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.SP, "entityId", null, true, false, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    @Test
    void updateIdentityProviderAllowAll() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true, true, new HashMap<>());
        current.metaDataFields().put("field1", "old");

        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));

        assertEquals("old", current.metaDataFields().get("field1"));

        autoRefreshRunner.run();

        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.IDP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.IDP), any());
        verify(metaDataService, times(1)).doPut(metaDataCaptor.capture(), anyString(), anyBoolean());

        MetaData result = metaDataCaptor.getValue();
        assertEquals("value", result.metaDataFields().get("field1"));
        assertEquals("value", result.metaDataFields().get("field2"));
        assertEquals("value", result.metaDataFields().get("field3"));
        assertFalse(result.metaDataFields().containsKey("field4"));
        assertEquals(MetadataAutoRefreshRunner.AUTO_REFRESH_REVISION_NOTE, result.getData().get(Revision.REVISION_KEY));
    }

    @Test
    void updateIdentityProviderAllowSubset() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true,
                false, Collections.singletonMap("field1", true));
        current.metaDataFields().put("field1", "old");
        current.metaDataFields().put("field2", "old");

        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));

        assertEquals("old", current.metaDataFields().get("field1"));
        assertEquals("old", current.metaDataFields().get("field2"));

        autoRefreshRunner.run();

        verify(metaDataAutoConfiguration, times(0)).schemaRepresentation(EntityType.IDP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.IDP), any());
        verify(metaDataService, times(1)).doPut(metaDataCaptor.capture(), anyString(), anyBoolean());

        MetaData result = metaDataCaptor.getValue();
        assertEquals("value", result.metaDataFields().get("field1"));
        assertEquals("old", result.metaDataFields().get("field2"));
        assertFalse(result.metaDataFields().containsKey("field3"));
        assertFalse(result.metaDataFields().containsKey("field4"));
        assertEquals(MetadataAutoRefreshRunner.AUTO_REFRESH_REVISION_NOTE, result.getData().get(Revision.REVISION_KEY));
    }

    @Test
    void updateIdentityProviderNoMatchingSubset() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true,
                false, Collections.singletonMap("field5", false));
        current.metaDataFields().put("field5", "old");
        current.metaDataFields().put("field6", "old");

        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));

        autoRefreshRunner.run();

        verify(metaDataAutoConfiguration, times(0)).schemaRepresentation(EntityType.IDP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.IDP), any());
        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
    }

    @Test
    void updateIdentityProviderRemoveFieldIfRemovedFromMetadata() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true,
                false, Collections.singletonMap("field5", true));
        current.metaDataFields().put("field5", "old");
        current.metaDataFields().put("field6", "old");

        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));

        autoRefreshRunner.run();

        verify(metaDataAutoConfiguration, times(0)).schemaRepresentation(EntityType.IDP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.IDP), any());
        verify(metaDataService, times(1)).doPut(metaDataCaptor.capture(), anyString(), anyBoolean());

        MetaData result = metaDataCaptor.getValue();
        assertFalse(result.metaDataFields().containsKey("field5"));
        assertEquals(MetadataAutoRefreshRunner.AUTO_REFRESH_REVISION_NOTE, result.getData().get(Revision.REVISION_KEY));
    }

    @Test
    void updateIdentityProviderDoNotAllowSubsetWithoutFields() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true, false, new HashMap<>());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    @Test
    void updateIdentityProviderNoChanges() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true, true, new HashMap<>());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.doPut(any(), anyString(), anyBoolean()))
                .thenThrow(new ValidationException(null, "No data is changed", ""));

        autoRefreshRunner.run();

        verify(metaDataService, times(1)).doPut(any(), anyString(), anyBoolean());
        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.IDP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.IDP), any());
    }

    @Test
    void updateIdentityProviderInvalidMetadata() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true, true, new HashMap<>());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());
        when(importerService.importXMLUrl(eq(EntityType.IDP), any())).thenReturn(Collections.singletonMap("errors", "invalid metadata"));

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.IDP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.IDP), any());
    }

    @Test
    void updateIdentityProviderValidationError() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true, true, new HashMap<>());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.doPut(any(), anyString(), anyBoolean()))
                .thenThrow(new ValidationException(null, "some error occurred", ""));

        autoRefreshRunner.run();

        verify(metaDataService, times(1)).doPut(any(), anyString(), anyBoolean());
        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.IDP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.IDP), any());
        assertTrue(memoryAppender.contains("Failed to save changes for saml20_idp entityId: #: some error occurred", Level.INFO));
    }

    @Test
    void updateIdentityProviderJsonProcessingError() throws JsonProcessingException {
        MetaData current = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", true, true, new HashMap<>());
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(current));
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());
        when(metaDataService.doPut(any(), anyString(), anyBoolean()))
                .thenThrow(new MockJsonProcessingException("some error occurred"));

        autoRefreshRunner.run();

        verify(metaDataService, times(1)).doPut(any(), anyString(), anyBoolean());
        verify(metaDataAutoConfiguration, times(1)).schemaRepresentation(EntityType.IDP);
        verify(importerService, times(1)).importXMLUrl(eq(EntityType.IDP), any());
        assertTrue(memoryAppender.contains("Failed to save changes for saml20_idp entityId: some error occurred", Level.INFO));
    }

    @Test
    void updateIdentityProviderNoAutoRefreshSettings() throws JsonProcessingException {
        MetaData metaData = buildMetadata(EntityType.IDP, "entityId", "metadataUrl", false, false, new HashMap<>());
        metaData.getData().put(MetadataAutoRefreshRunner.AUTO_REFRESH_KEY, null);
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(metaData));
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    @Test
    void updateIdentityProviderAutoRefreshDisabled() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.IDP, "entityId", "metadataUrl", false, false, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    @Test
    void updateIdentityProviderNoMetadataUrl() throws JsonProcessingException {
        when(metaDataService.findAllByType(EntityType.IDP.getType())).thenReturn(Collections.singletonList(
                buildMetadata(EntityType.IDP, "entityId", null, true, false, new HashMap<>())
        ));
        when(metaDataService.findAllByType(EntityType.SP.getType())).thenReturn(Collections.emptyList());

        autoRefreshRunner.run();

        verify(metaDataService, times(0)).doPut(any(), anyString(), anyBoolean());
        verifyNoInteractions(metaDataAutoConfiguration);
        verifyNoInteractions(importerService);
    }

    private MetaData buildMetadata(EntityType type, String entityId, String metadataUrl, boolean enableAutoRefresh,
                                   boolean allowAll, Map<String, Object> allowedFields) {

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> autoRefreshData = new HashMap<>();
        autoRefreshData.put("enabled", enableAutoRefresh);
        autoRefreshData.put("allowAll", allowAll);
        autoRefreshData.put(MetadataAutoRefreshRunner.FIELDS_KEY, allowedFields);
        data.put(MetadataAutoRefreshRunner.METADATA_ENTITYID_KEY, entityId);
        data.put(MetadataAutoRefreshRunner.METADATA_URL_KEY, metadataUrl);
        data.put(MetadataAutoRefreshRunner.AUTO_REFRESH_KEY, autoRefreshData);
        data.put(MetadataAutoRefreshRunner.METADATA_FIELDS_KEY, new HashMap<>());
        return new MetaData(type.getType(), data);
    }

    private static class MockJsonProcessingException extends JsonProcessingException {
        public MockJsonProcessingException(String message) {
            super(message);
        }
    }

}
