package manage.control;

import manage.api.APIUser;
import manage.conf.Features;
import manage.exception.EndpointNotAllowed;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.OrphanMetaData;
import manage.model.PushOptions;
import manage.repository.MetaDataRepository;
import manage.shibboleth.FederatedUser;
import manage.validations.MetaDataValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@RestController
@SuppressWarnings("unchecked")
public class SystemController {

    private static final Logger LOG = LoggerFactory.getLogger(SystemController.class);

    private final MetaDataRepository metaDataRepository;
    private final MetaDataValidator metaDataValidator;
    private final DatabaseController databaseController;

    @Autowired
    public SystemController(MetaDataRepository metaDataRepository,
                            MetaDataValidator metaDataValidator,
                            DatabaseController databaseController) {
        this.metaDataRepository = metaDataRepository;
        this.metaDataValidator = metaDataValidator;
        this.databaseController = databaseController;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/playground/push")
    public ResponseEntity<Map> push(@RequestBody PushOptions pushOptions, FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.PUSH)) {
            throw new EndpointNotAllowed();
        }
        return databaseController.doPush(pushOptions);
    }

    @PreAuthorize("hasRole('PUSH')")
    @GetMapping("/internal/push")
    public ResponseEntity<Map> pushInternal(APIUser apiUser) {
        LOG.info("Push initiated by {}", apiUser.getName());
        return databaseController.doPush(new PushOptions(true, true, false));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/validate")
    public Map<String, Object> validate(FederatedUser federatedUser) {
        if (!federatedUser.featureAllowed(Features.VALIDATION)) {
            throw new EndpointNotAllowed();
        }
        return metaDataValidator.validateMigration();
    }

    @PreAuthorize("hasRole('SYSTEM')")
    @DeleteMapping({"/client/playground/deleteOrphans"})
    public void deleteOrphans() {
        doDeleteOrphans();
    }

    @PreAuthorize("hasRole('SYSTEM')")
    @DeleteMapping({"/internal/playground/deleteOrphans"})
    public void deleteOrphansInternal() {
        doDeleteOrphans();
    }

    private void doDeleteOrphans() {
        List<OrphanMetaData> orphans = this.orphans();
        orphans.forEach(orphanMetaData -> {
            MetaData metaData = metaDataRepository.findById(orphanMetaData.getId(), orphanMetaData.getCollection());
            List<Map<String, Object>> entries =
                    (List<Map<String, Object>>) metaData.getData().get(orphanMetaData.getReferencedCollectionName());

            List<Map<String, Object>> newEntries = entries.stream().filter(entry -> !entry.get("name").equals
                    (orphanMetaData.getMissingEntityId())).collect(Collectors.toList());
            metaData.getData().put(orphanMetaData.getReferencedCollectionName(), newEntries);
            MetaData previous = metaDataRepository.findById(metaData.getId(), orphanMetaData.getCollection());
            previous.revision(UUID.randomUUID().toString());
            metaDataRepository.save(previous);
            metaData.promoteToLatest("System", "Removed reference to non-existent entityID");
            metaDataRepository.update(metaData);

        });
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @GetMapping({"/client/playground/orphans", "/internal/playground/orphans"})
    public List<OrphanMetaData> orphans() {
        return Stream.of(EntityType.values())
                .flatMap(this::orphanMetaData)
                .collect(toList());
    }

    private Stream<OrphanMetaData> orphanMetaData(EntityType type) {
        Query query = new Query();
        query.fields()
                .include("data.entityid")
                .include("type")
                .include("data.metaDataFields.name:en")
                .include("data.name")
                .include("data.allowedEntities.name")
                .include("data.identityProviderIds.name")
                .include("data.serviceProviderIds.name")
                .include("data.allowedResourceServers.name")
                .include("data.stepupEntities.name")
                .include("data.mfaEntities.name")
                .include("data.disableConsent.name");

        query.addCriteria(new Criteria().orOperator(
                Criteria.where("data.allowedEntities").exists(true),
                Criteria.where("data.disableConsent").exists(true),
                Criteria.where("data.stepupEntities").exists(true),
                Criteria.where("data.serviceProviderIds").exists(true),
                Criteria.where("data.identityProviderIds").exists(true),
                Criteria.where("data.mfaEntities").exists(true),
                Criteria.where("data.allowedResourceServers").exists(true)));

        MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
        List<MetaData> metaDataWithReferences = mongoTemplate.find(query, MetaData.class, type.getType());

        Map<String, Map<String, List<MetaData>>> groupedByEntityIdReference = new HashMap<>();
        Stream.of("allowedEntities", "disableConsent", "stepupEntities", "identityProviderIds", "serviceProviderIds", "mfaEntities", "allowedResourceServers").forEach(propertyName -> {
            metaDataWithReferences.stream().forEach(metaData -> {
                List<Map<String, Object>> entries = (List<Map<String, Object>>) metaData.getData().get(propertyName);
                if (!CollectionUtils.isEmpty(entries)) {
                    entries.forEach(ae -> groupedByEntityIdReference
                            .computeIfAbsent((String) ae.get("name"), k -> new HashMap<>())
                            .computeIfAbsent(propertyName, m -> new ArrayList<>())
                            .add(metaData));
                }
            });
        });
        List<String> types = Arrays.stream(EntityType.values()).map(EntityType::getType).collect(toList());
        return groupedByEntityIdReference.entrySet().stream()
                .filter(entry -> types.stream()
                        .noneMatch(entityType -> mongoTemplate.exists(new BasicQuery("{\"data.entityid\":\"" + entry.getKey() + "\"}"), entityType)))
                .flatMap(entry -> entry.getValue().entrySet().stream().map(m ->
                        m.getValue().stream().map(metaData -> new OrphanMetaData(
                                entry.getKey(),
                                (String) metaData.getData().get("entityid"),
                                type.equals(EntityType.PDP) ? (String) metaData.getData().get("name") :
                                        (String) metaData.metaDataFields().get("name:en"),
                                m.getKey(),
                                metaData.getId(),
                                type.getType()
                        ))))
                .flatMap(Function.identity());
    }
}
