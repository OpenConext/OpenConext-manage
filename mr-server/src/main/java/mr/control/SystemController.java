package mr.control;

import mr.conf.Features;
import mr.exception.EndpointNotAllowed;
import mr.format.EngineBlockFormatter;
import mr.migration.JanusMigration;
import mr.migration.JanusMigrationValidation;
import mr.model.MetaData;
import mr.repository.MetaDataRepository;
import mr.shibboleth.FederatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SystemController {

    private URI pushUri;
    private JanusMigrationValidation janusMigrationValidation;
    private JanusMigration janusMigration;
    private MetaDataRepository metaDataRepository;

    @Autowired
    public SystemController(JanusMigration janusMigration,
                            JanusMigrationValidation janusMigrationValidation,
                            MetaDataRepository metaDataRepository,
                            @Value("${push.url}") URI pushUri) {
        this.janusMigration = janusMigration;
        this.janusMigrationValidation = janusMigrationValidation;
        this.metaDataRepository = metaDataRepository;
        this.pushUri = pushUri;
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
    public ResponseEntity<Map> push(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.PUSH)) {
            throw new EndpointNotAllowed();
        }
        if (this.pushUri.toString().contains("localhost")) {
            return new ResponseEntity<>(Collections.singletonMap("status", HttpStatus.BAD_REQUEST), HttpStatus.OK);
        }
        Map<String, Map<String, Map<String, Object>>> json = this.pushPreview(federatedUser);
        ResponseEntity<Void> response = new RestTemplate().postForEntity(pushUri, json, Void.class);
        HttpStatus statusCode = response.getStatusCode();
        return new ResponseEntity<>(Collections.singletonMap("status", statusCode), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/validate")
    public Map<String, Object> validate(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.VALIDATION)) {
            throw new EndpointNotAllowed();
        }
        return janusMigrationValidation.validateMigration();
    }
}
