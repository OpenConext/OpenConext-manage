package manage.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import manage.conf.Features;
import manage.conf.Product;
import manage.conf.Push;
import manage.exception.EndpointNotAllowed;
import manage.format.EngineBlockFormatter;
import manage.mail.MailBox;
import manage.migration.JanusMigration;
import manage.migration.JanusMigrationValidation;
import manage.model.MetaData;
import manage.push.Delta;
import manage.push.PrePostComparator;
import manage.repository.MetaDataRepository;
import manage.shibboleth.FederatedUser;
import manage.web.PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.mail.MessagingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.Executors.newScheduledThreadPool;
import static java.util.stream.Collectors.toList;

@RestController
public class SystemController {

    private static final FederatedUser FEDERATED_USER = new FederatedUser(
        "system",
        "system",
        "system",
        AuthorityUtils.createAuthorityList("ROLE_ADMIN"),
        Arrays.asList(Features.values()),
        Product.DEFAULT,
        new Push("https://nope", "push"));

    private RestTemplate restTemplate;
    private String pushUser;
    private String pushPassword;
    private String pushUri;
    private JanusMigrationValidation janusMigrationValidation;
    private JanusMigration janusMigration;
    private MetaDataRepository metaDataRepository;
    private JdbcTemplate ebJdbcTemplate;
    private Environment environment;
    private ObjectMapper objectMapper;
    private MailBox mailBox;

    private PrePostComparator prePostComparator = new PrePostComparator();

    @Autowired
    public SystemController(JanusMigration janusMigration,
                            JanusMigrationValidation janusMigrationValidation,
                            MetaDataRepository metaDataRepository,
                            @Qualifier("ebDataSource") DataSource ebDataSource,
                            @Value("${push.url}") String pushUri,
                            @Value("${push.user}") String user,
                            @Value("${push.password}") String password,
                            @Value("${manage_cronjob_minutes}") int everyMinutes,
                            @Value("${manage_cronjobmaster}") boolean cronJobResponsible,
                            Environment environment,
                            ObjectMapper objectMapper,
                            MailBox mailBox) throws MalformedURLException {
        this.janusMigration = janusMigration;
        this.janusMigrationValidation = janusMigrationValidation;
        this.metaDataRepository = metaDataRepository;
        this.pushUri = pushUri;
        this.pushUser = user;
        this.pushPassword = password;
        this.restTemplate = new RestTemplate(getRequestFactory());
        this.ebJdbcTemplate = new JdbcTemplate(ebDataSource);
        this.environment = environment;
        this.objectMapper = objectMapper;
        this.mailBox = mailBox;

        if (cronJobResponsible) {
            newScheduledThreadPool(1)
                .scheduleAtFixedRate(() -> migrateAndPush(), 0, everyMinutes, TimeUnit.MINUTES);
        }
    }

    private void migrateAndPush() {
        try {
            janusMigration.doMigrate();
            ResponseEntity<Map> responseEntity = this.push(FEDERATED_USER);
            List<Delta> realDeltas = (List<Delta>) responseEntity.getBody().get("deltas");
            if (!responseEntity.getStatusCode().equals(HttpStatus.OK) || !realDeltas.isEmpty()) {
                mailBox.sendDeltaPushMail(realDeltas);
            }
        } catch (IOException | RuntimeException | MessagingException e) {
            //don't break the scheduler
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/migrate")
    public List<Map<String, Long>> migrate(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.MIGRATION)) {
            throw new EndpointNotAllowed();
        }
        return janusMigration.doMigrate();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/pushPreview")
    public Map<String, Map<String, Map<String, Object>>> pushPreview(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.PUSH_PREVIEW)) {
            throw new EndpointNotAllowed();
        }

        EngineBlockFormatter formatter = new EngineBlockFormatter();

        Map<String, Map<String, Map<String, Object>>> results =
            new HashMap<>();
        Map<String, Map<String, Object>> connections = new HashMap<>();
        results.put("connections", connections);

        List<MetaData> serviceProviders = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_sp");
        serviceProviders.forEach(sp ->
            connections.put(sp.getId(), formatter.parseServiceProvider(sp)));

        List<MetaData> identityProviders = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "saml20_idp");
        identityProviders.forEach(idp ->
            connections.put(idp.getId(), formatter.parseIdentityProvider(idp)));
        return results;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/push")
    public ResponseEntity<Map> push(FederatedUser federatedUser) throws IOException {
        if (!federatedUser.featureAllowed(Features.PUSH)) {
            throw new EndpointNotAllowed();
        }
        if (environment.acceptsProfiles("dev")) {
            Map map = objectMapper.readValue(new ClassPathResource("mock/mock_eb_push_repsonse.json").getInputStream(), Map.class);
            return new ResponseEntity<>(map, HttpStatus.OK);
        }
        List<Map<String, Object>> preProvidersData = ebJdbcTemplate.queryForList("SELECT * FROM sso_provider_roles_eb5 ORDER BY id ASC");

        Map<String, Map<String, Map<String, Object>>> json = this.pushPreview(federatedUser);
        ResponseEntity<String> response = this.restTemplate.postForEntity(pushUri, json, String.class);
        HttpStatus statusCode = response.getStatusCode();

        List<Map<String, Object>> postProvidersData = ebJdbcTemplate.queryForList("SELECT * FROM sso_provider_roles_eb5 ORDER BY id ASC");
        Set<Delta> deltas = prePostComparator.compare(preProvidersData, postProvidersData);

        List<String> knownDeltas = Arrays.asList("id", "name_id_formats", "attribute_release_policy", "allowed_idp_entity_ids");
        List<Delta> realDeltas = deltas.stream().filter(delta -> !knownDeltas.contains(delta.getAttribute())).collect(toList());
        realDeltas.sort(Comparator.comparing(Delta::getEntityId));

        Map<String, Object> result = new HashMap<>();
        result.put("status", statusCode);
        result.put("response", response);
        result.put("deltas", realDeltas);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/validate")
    public Map<String, Object> validate(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.VALIDATION)) {
            throw new EndpointNotAllowed();
        }
        return janusMigrationValidation.validateMigration();
    }

    private ClientHttpRequestFactory getRequestFactory() throws MalformedURLException {
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create().evictExpiredConnections().evictIdleConnections(10l, TimeUnit.SECONDS);
        BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
        basicCredentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(pushUser, pushPassword));
        httpClientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);
        //httpClientBuilder.setDefaultRequestConfig(RequestConfig.custom().setConnectionRequestTimeout(timeOut).setConnectTimeout(timeOut).setSocketTimeout(timeOut).build());

        CloseableHttpClient httpClient = httpClientBuilder.build();
        return new PreemptiveAuthenticationHttpComponentsClientHttpRequestFactory(httpClient, pushUri);
    }


}
