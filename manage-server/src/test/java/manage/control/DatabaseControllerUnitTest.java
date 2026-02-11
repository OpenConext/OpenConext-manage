package manage.control;

import manage.model.PushOptions;
import manage.repository.MetaDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

public class DatabaseControllerUnitTest {

    private DatabaseController subject;
    private MetaDataRepository metaDataRepository = Mockito.mock(MetaDataRepository.class);
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate = Mockito.mock(org.springframework.data.mongodb.core.MongoTemplate.class);
    private Environment environment = Mockito.mock(Environment.class);
    private RestTemplate pdpRestTemplate = Mockito.mock(RestTemplate.class);
    private RestTemplate ebRestTemplate = Mockito.mock(RestTemplate.class);
    private RestTemplate oidcRestTemplate = Mockito.mock(RestTemplate.class);

    @BeforeEach
    public void before() {
        when(metaDataRepository.getMongoTemplate()).thenReturn(mongoTemplate);
        subject = new DatabaseController(
                metaDataRepository,
                "http://eb-push", "user", "pass", false, false, false,
                "http://oidc-push", "user", "pass",
                "http://pdp-push", "user", "pass",
                true, true, environment);

        ReflectionTestUtils.setField(subject, "pdpRestTemplate", pdpRestTemplate);
        ReflectionTestUtils.setField(subject, "restTemplate", ebRestTemplate);
        ReflectionTestUtils.setField(subject, "oidcRestTemplate", oidcRestTemplate);

        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
    }

    @Test
    public void doPushPdpError() {
        HttpServerErrorException exception = HttpServerErrorException.create(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                null,
                "{\"error\":\"pdp error\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        doThrow(exception).when(pdpRestTemplate).put(eq("http://pdp-push"), anyList());

        ResponseEntity<Map> response = subject.doPush(new PushOptions(false, false, true));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("Error in push to PDP (http://pdp-push) status 500 INTERNAL_SERVER_ERROR and response {\"error\":\"pdp error\"}"));
    }

    @Test
    public void doPushPdpServiceUnavailable() {
        HttpServerErrorException exception = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                null,
                "".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        doThrow(exception).when(pdpRestTemplate).put(eq("http://pdp-push"), anyList());

        ResponseEntity<Map> response = subject.doPush(new PushOptions(false, false, true));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("Error in push to PDP (http://pdp-push) status 503 SERVICE_UNAVAILABLE"));
    }

    @Test
    public void doPushPdpGenericError() {
        doThrow(new RuntimeException("Connection refused")).when(pdpRestTemplate).put(eq("http://pdp-push"), anyList());

        ResponseEntity<Map> response = subject.doPush(new PushOptions(false, false, true));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("Error in push to PDP (http://pdp-push) error Connection refused"));
    }

    @Test
    public void doPushSuccess() {
        when(mongoTemplate.stream(any(), any(), anyString())).thenAnswer(invocation -> java.util.stream.Stream.empty());
        when(ebRestTemplate.postForEntity(eq("http://eb-push"), anyMap(), eq(String.class))).thenReturn(new ResponseEntity<>("OK", HttpStatus.OK));

        ResponseEntity<Map> response = subject.doPush(new PushOptions(true, false, false));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> ebResult = (Map<String, Object>) response.getBody().get("eb");
        assertEquals("OK", ebResult.get("status"));
    }

    @Test
    public void doPushEbError() {
        HttpServerErrorException exception = HttpServerErrorException.create(
                HttpStatus.BAD_GATEWAY,
                "Bad Gateway",
                null,
                "{\"error\":\"eb error\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(mongoTemplate.stream(any(), any(), anyString())).thenAnswer(invocation -> java.util.stream.Stream.empty());
        when(ebRestTemplate.postForEntity(eq("http://eb-push"), anyMap(), eq(String.class))).thenThrow(exception);

        ResponseEntity<Map> response = subject.doPush(new PushOptions(true, false, false));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("Error in push to EngineBlock (http://eb-push) status 502 BAD_GATEWAY and response {\"error\":\"eb error\"}"));
    }

    @Test
    public void doPushEbServiceUnavailable() {
        HttpServerErrorException exception = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                null,
                "".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(mongoTemplate.stream(any(), any(), anyString())).thenAnswer(invocation -> java.util.stream.Stream.empty());
        when(ebRestTemplate.postForEntity(eq("http://eb-push"), anyMap(), eq(String.class))).thenThrow(exception);

        ResponseEntity<Map> response = subject.doPush(new PushOptions(true, false, false));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("Error in push to EngineBlock (http://eb-push) status 503 SERVICE_UNAVAILABLE"));
    }

    @Test
    public void doPushPdpForbidden() {
        HttpClientErrorException exception = HttpClientErrorException.create(
                HttpStatus.FORBIDDEN,
                "Forbidden",
                null,
                "".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        doThrow(exception).when(pdpRestTemplate).put(eq("http://pdp-push"), anyList());

        ResponseEntity<Map> response = subject.doPush(new PushOptions(false, false, true));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("Error in push to PDP (http://pdp-push) status 403 FORBIDDEN"));
    }

    @Test
    public void doPushOidcError() {
        HttpServerErrorException exception = HttpServerErrorException.create(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable",
                null,
                "{\"error\":\"oidc error\"}".getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8);

        when(mongoTemplate.findAll(any(), anyString())).thenReturn(java.util.Collections.emptyList());
        when(oidcRestTemplate.postForEntity(eq("http://oidc-push"), anyList(), eq(Void.class))).thenThrow(exception);

        ResponseEntity<Map> response = subject.doPush(new PushOptions(false, true, false));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        String message = (String) response.getBody().get("message");
        assertTrue(message.contains("Error in push to OIDC (http://oidc-push) status 503 SERVICE_UNAVAILABLE and response {\"error\":\"oidc error\"}"));
    }
}
