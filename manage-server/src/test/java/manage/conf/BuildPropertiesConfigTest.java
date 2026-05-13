package manage.conf;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BuildPropertiesConfigTest {

    @Mock
    private ResourceLoader resourceLoader;

    @InjectMocks
    private final BuildPropertiesConfig buildPropertiesConfig = new BuildPropertiesConfig();


    @Test
    void buildPropertiesMissing() {
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(false);
        when(resourceLoader.getResource(any())).thenReturn(mockResource);

        BuildProperties buildProperties = buildPropertiesConfig.buildProperties(resourceLoader);

        assertEquals("0.0.0-TEST", buildProperties.getVersion());
        assertEquals("test-build", buildProperties.getName());
        assertEquals("test-group", buildProperties.getGroup());
        assertEquals("test-artifact", buildProperties.getArtifact());
    }

    @Test
    void buildPropertiesAvailable() throws IOException {
        Resource mockResource = mock(Resource.class);

        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenReturn(
                new ByteArrayInputStream((
                        """
                                build.artifact=manage-server
                                build.group=org.openconext
                                build.java_version=21
                                build.name=manage-server
                                build.spring_boot_version=3.5.7
                                build.time=2026-05-13T14\\:50\\:24.823Z
                                build.version=9.7.3-SNAPSHOT""").getBytes()));

        when(resourceLoader.getResource(any())).thenReturn(mockResource);

        BuildProperties buildProperties = buildPropertiesConfig.buildProperties(resourceLoader);

        assertEquals("manage-server", buildProperties.getArtifact());
        assertEquals("org.openconext", buildProperties.getGroup());
        assertEquals("manage-server", buildProperties.getName());
        assertEquals("9.7.3-SNAPSHOT", buildProperties.getVersion());
        assertEquals(Instant.parse("2026-05-13T14:50:24.823Z"), buildProperties.getTime());
        assertEquals("21", buildProperties.get("java_version"));
        assertEquals("3.5.7", buildProperties.get("spring_boot_version"));
    }

    @Test
    void buildPropertiesMissingIOException() throws IOException {
        Resource mockResource = mock(Resource.class);

        when(mockResource.exists()).thenReturn(true);
        doThrow(IOException.class).when(mockResource).getInputStream();

        when(resourceLoader.getResource(any())).thenReturn(mockResource);

        BuildProperties buildProperties = buildPropertiesConfig.buildProperties(resourceLoader);

        assertEquals("0.0.0-ERROR", buildProperties.getVersion());
        assertEquals("error-build", buildProperties.getName());
        assertEquals("error-group", buildProperties.getGroup());
        assertEquals("error-artifact", buildProperties.getArtifact());
    }

}
