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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@RestController
public class SystemController {

    private JanusMigrationValidation janusMigrationValidation;
    private JanusMigration janusMigration;
    private MetaDataRepository metaDataRepository;

    @Autowired
    public SystemController(JanusMigration janusMigration,
                            JanusMigrationValidation janusMigrationValidation,
                            MetaDataRepository metaDataRepository) {
        this.janusMigration = janusMigration;
        this.janusMigrationValidation = janusMigrationValidation;
        this.metaDataRepository = metaDataRepository;
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
    @GetMapping("/client/playground/push")
    public Map<String, Map<String, Map<String, Object>>> push(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.PUSH)) {
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
    @GetMapping("/client/playground/validate")
    public Map<String, Object> validate(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.VALIDATION)) {
            throw new EndpointNotAllowed();
        }
        return janusMigrationValidation.validateMigration();
    }
}
