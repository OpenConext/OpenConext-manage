package manage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.result.DeleteResult;
import lombok.SneakyThrows;
import manage.api.APIUser;
import manage.api.AbstractUser;
import manage.api.Scope;
import manage.conf.MetaDataAutoConfiguration;
import manage.control.DatabaseController;
import manage.exception.DuplicateEntityIdException;
import manage.exception.EndpointNotAllowed;
import manage.exception.ResourceNotFoundException;
import manage.format.SaveURLResource;
import manage.hook.MetaDataHook;
import manage.model.*;
import manage.repository.MetaDataRepository;
import manage.shibboleth.FederatedUser;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;

@Service
@SuppressWarnings("unchecked")
public class MetaDataService {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataService.class);

    private static final APIUser EDUGAIN_IMPORT_USER = new APIUser("edugain-import", List.of(Scope.SYSTEM));

    public static final String REQUESTED_ATTRIBUTES = "REQUESTED_ATTRIBUTES";

    public static final String ALL_ATTRIBUTES = "ALL_ATTRIBUTES";

    public static final String LOGICAL_OPERATOR_IS_AND = "LOGICAL_OPERATOR_IS_AND";

    private static final String DASHBOARD_CONNECT_OPTION = "coin:dashboard_connect_option";

    private static final List<String> entityTypesSuggestions = Arrays.asList(
            EntityType.RP.getType(), EntityType.SP.getType(), EntityType.RS.getType()
    );
    private final MetaDataRepository metaDataRepository;

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    private final MetaDataHook metaDataHook;

    private final DatabaseController databaseController;

    private final Environment environment;

    private final ImporterService importerService;

    private final ExporterService exporterService;

    public MetaDataService(MetaDataRepository metaDataRepository,
                           MetaDataAutoConfiguration metaDataAutoConfiguration,
                           MetaDataHook metaDataHook,
                           DatabaseController databaseController,
                           ImporterService importerService,
                           ExporterService exporterService,
                           Environment environment) {

        this.metaDataRepository = metaDataRepository;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataHook = metaDataHook;
        this.databaseController = databaseController;
        this.exporterService = exporterService;
        this.environment = environment;
        this.importerService = importerService;
    }

    public MetaData getMetaDataAndValidate(String type, String id) {
        MetaData metaData = metaDataRepository.findById(id, type);
        checkNull(type, id, metaData);
        return metaDataHook.postGet(metaData);
    }

    public Map<String, List> importFeed(Import importRequest) {
        try {
            Map<String, ServiceProvider> serviceProviderMap =
                    metaDataRepository.allServiceProviderEntityIds().stream()
                            .map(ServiceProvider::new)
                            .collect(Collectors.toMap(ServiceProvider::getEntityId, sp -> sp));
            String feedUrl = importRequest.getUrl();
            Resource resource = new SaveURLResource(new URL(feedUrl), environment.acceptsProfiles(Profiles.of("dev")), null);

            List<Map<String, Object>> allImports = importerService.importFeed(resource);
            List<Map<String, Object>> imports =
                    allImports.stream().filter(m -> !m.isEmpty()).collect(toList());

            Map<String, List> results = new HashMap<>();
            EntityType entityType = EntityType.SP;
            imports.forEach(sp -> {
                String entityId = (String) sp.get("entityid");
                sp.put("metadataurl", feedUrl);
                Map metaDataFields = Map.class.cast(sp.get("metaDataFields"));
                metaDataFields.put("coin:imported_from_edugain", true);
                metaDataFields.put("coin:interfed_source", "eduGAIN");

                ServiceProvider existingServiceProvider = serviceProviderMap.get(entityId);
                if (existingServiceProvider != null) {
                    if (existingServiceProvider.isPublishedInEduGain()) {
                        // Do not import this SP as it's source is SURFconext
                        List publishedInEdugain = results.computeIfAbsent("published_in_edugain", s -> new ArrayList());
                        publishedInEdugain.add(existingServiceProvider);
                    } else if (existingServiceProvider.isImportedFromEduGain()) {
                        try {
                            MetaDataUpdate metaDataUpdate =
                                    importToMetaDataUpdate(existingServiceProvider.getId(), entityType, sp, feedUrl);
                            Optional<MetaData> metaData = doMergeUpdate(metaDataUpdate,
                                    EDUGAIN_IMPORT_USER,
                                    "edugain-import",
                                    false);
                            if (metaData.isPresent()) {
                                List merged = results.computeIfAbsent("merged", s -> new ArrayList());
                                merged.add(existingServiceProvider);
                            } else {
                                List noChanges = results.computeIfAbsent("no_changes", s -> new ArrayList());
                                noChanges.add(existingServiceProvider);
                            }
                        } catch (JsonProcessingException | ValidationException e) {
                            addNoValid(results, entityId, e);
                        }
                    } else {
                        // Do not import this SP as it is modified after the import or is not imported at all
                        List notImported = results.computeIfAbsent("not_imported", s -> new ArrayList());
                        notImported.add(existingServiceProvider);
                    }
                } else {
                    try {
                        MetaData metaData = importToMetaData(sp, entityType);
                        MetaData persistedMetaData = doPost(metaData, EDUGAIN_IMPORT_USER, false);
                        List imported = results.computeIfAbsent("imported", s -> new ArrayList());
                        imported.add(new ServiceProvider(persistedMetaData.getId(), entityId, false, false, null));
                    } catch (RuntimeException e) {
                        addNoValid(results, entityId, e);
                    }
                }
            });
            List<ServiceProvider> notInFeedAnymore = serviceProviderMap.values().stream()
                    .filter(sp -> sp.isImportedFromEduGain() &&
                            imports.stream().noneMatch(map -> sp.getEntityId().equals(map.get("entityid"))))
                    .collect(toList());
            notInFeedAnymore.forEach(sp -> doRemove(entityType.getType(),
                    sp.getId(),
                    EDUGAIN_IMPORT_USER,
                    "Removed from eduGain feed"));

            List deleted = results.computeIfAbsent("deleted", s -> new ArrayList<>());
            deleted.addAll(notInFeedAnymore.stream().map(ServiceProvider::getEntityId).collect(toList()));

            results.put("total", Collections.singletonList(imports.size()));

            return results;
        } catch (IOException | XMLStreamException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
        }
    }

    @SneakyThrows
    public MetaData doPost(@Validated MetaData metaData, AbstractUser user, boolean excludeFromPushRequired)
            {
        metaData = metaDataHook.prePost(metaData, user);
        checkForDuplicateEntityId(metaData, true);

        sanitizeExcludeFromPush(metaData, excludeFromPushRequired);

        metaData = validate(metaData);
        Long eid = metaDataRepository.incrementEid();
        metaData.initial(UUID.randomUUID().toString(), user.getName(), eid);

        LOG.info("Saving new metaData {} by {}", metaData.getId(), user.getName());

        metaDataRepository.save(metaData);

        return getMetaDataAndValidate(metaData.getType(), metaData.getId());
    }

    public boolean doRemove(String type, String id, AbstractUser user, String revisionNote) {
        MetaData current = metaDataRepository.findById(id, type);
        checkNull(type, id, current);
        //For security enforcement see the SecurityHook#preDelete
        current = metaDataHook.preDelete(current, user);
        metaDataRepository.remove(current);

        LOG.info("Deleted metaData {} by {}", current.getId(), user.getName());

        String changeRequestCollection = type.concat(CHANGE_REQUEST_POSTFIX);
        Criteria criteria = Criteria.where("metaDataId").is(id);
        MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
        List<MetaDataChangeRequest> changeRequests = mongoTemplate
                .findAllAndRemove(Query.query(criteria), MetaDataChangeRequest.class, changeRequestCollection);

        LOG.info("Deleted changeRequests {} by {}",
                changeRequests.stream().map(MetaDataChangeRequest::getId).collect(Collectors.joining()),
                user.getName());

        current.revision(UUID.randomUUID().toString());
        metaDataRepository.save(current);

        current.terminate(UUID.randomUUID().toString(), revisionNote, user.getName());
        metaDataRepository.save(current);
        return true;
    }

    public MetaData doPut(@Validated MetaData metaData, AbstractUser user, boolean excludeFromPushRequired)
            throws JsonProcessingException {
        checkForDuplicateEntityId(metaData, false);

        sanitizeExcludeFromPush(metaData, excludeFromPushRequired);
        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());
        checkNull(metaData.getType(), id, previous);

        metaData = metaDataHook.prePut(previous, metaData, user);
        metaData = validate(metaData);

        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest(user.getName(), (String) metaData.getData().get("revisionnote"));
        metaDataRepository.update(metaData);

        LOG.info("Updated metaData {} by {}", metaData.getId(), user.getName());

        return getMetaDataAndValidate(metaData.getType(), metaData.getId());
    }

    public List<String> deleteMetaDataKey(MetaDataKeyDelete metaDataKeyDelete, APIUser apiUser)
            throws JsonProcessingException {
        String keyToDelete = metaDataKeyDelete.getMetaDataKey();
        Query query = Query.query(Criteria.where("data.metaDataFields." + keyToDelete).exists(true));
        List<MetaData> metaDataList = metaDataRepository.getMongoTemplate()
                .find(query, MetaData.class, metaDataKeyDelete.getType());

        //of we stream then we need to catch all exceptions including validation exception
        for (MetaData metaData : metaDataList) {
            metaData.metaDataFields().remove(keyToDelete);
            metaData = validate(metaData);

            MetaData previous = metaDataRepository.findById(metaData.getId(), metaData.getType());
            previous.revision(UUID.randomUUID().toString());
            metaDataRepository.save(previous);

            metaData.promoteToLatest(apiUser.getName(),
                    String.format("API call for deleting %s by %s", keyToDelete, apiUser.getName()));
            metaDataRepository.update(metaData);
        }

        return metaDataList.stream().map(metaData -> (String) metaData.getData().get("entityid")).collect(toList());
    }

    public Optional<MetaData> doMergeUpdate(PathUpdates metaDataUpdate,
                                            AbstractUser user,
                                            String revisionNote,
                                            boolean forceNewRevision) throws JsonProcessingException {
        String id = metaDataUpdate.getMetaDataId();
        MetaData previous = metaDataRepository.findById(id, metaDataUpdate.getType());
        checkNull(metaDataUpdate.getType(), id, previous);

        previous.revision(UUID.randomUUID().toString());

        MetaData metaData = metaDataRepository.findById(id, metaDataUpdate.getType());
        metaData.promoteToLatest(user.getName(), revisionNote);
        metaData.merge(metaDataUpdate);

        if (!CollectionUtils.isEmpty(metaDataUpdate.getExternalReferenceData())) {
            metaData.getData().putAll(metaDataUpdate.getExternalReferenceData());
        }
        if (!user.getName().equals("edugain-import")) {
            metaData = metaDataHook.prePut(previous, metaData, user);
        }
        metaData = validate(metaData);
        //Only save and update if there are changes
        boolean somethingChanged = !metaData.metaDataFields().equals(previous.metaDataFields());

        if (somethingChanged || forceNewRevision) {
            metaDataRepository.save(previous);
            metaDataRepository.update(metaData);

            LOG.info("Merging new metaData {} by {}", metaData.getId(), user.getName());

            return Optional.of(getMetaDataAndValidate(metaData.getType(), metaData.getId()));
        } else {
            return Optional.empty();
        }
    }

    public MetaDataChangeRequest doChangeRequest(MetaDataChangeRequest metaDataChangeRequest, AbstractUser user) throws JsonProcessingException {
        String id = metaDataChangeRequest.getMetaDataId();
        MetaData metaData = metaDataRepository.findById(id, metaDataChangeRequest.getType());
        checkNull(metaDataChangeRequest.getType(), id, metaData);

        metaData.merge(metaDataChangeRequest);
        //fail fast if there are validation errors
        validate(metaData);

        metaDataChangeRequest.getAuditData().put("userName", user.getName());
        metaDataChangeRequest.getAuditData().put("apiUser", user.isAPIUser());
        metaDataChangeRequest.setMetaDataSummary(metaData.summary());
        metaDataChangeRequest.setCreated(Instant.now());

        return metaDataRepository.save(metaDataChangeRequest);
    }


    public MetaData doRejectChangeRequest(ChangeRequest changeRequest, AbstractUser user) {
        LOG.info("Rejecting change request {} by {}", changeRequest.getType(), user.getName());

        MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
        String collectionName = changeRequest.getType().concat(CHANGE_REQUEST_POSTFIX);
        MetaDataChangeRequest request = mongoTemplate.findById(changeRequest.getId(), MetaDataChangeRequest.class, collectionName);
        mongoTemplate.remove(request, collectionName);

        return mongoTemplate.findById(changeRequest.getMetaDataId(), MetaData.class, changeRequest.getType());
    }

    public DeleteResult doDeleteChangeRequest(String type, String metaDataId, AbstractUser user) {
        LOG.info("Deleting change requests {} by {}", metaDataId, user.getName());

        MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
        String collectionName = type.concat(CHANGE_REQUEST_POSTFIX);
        Query query = new Query(Criteria.where("metaDataId").is(metaDataId));
        return mongoTemplate.remove(query, MetaDataChangeRequest.class, collectionName);
    }

    public MetaData restoreDeleted(RevisionRestore revisionRestore, FederatedUser federatedUser)
            throws JsonProcessingException {
        MetaData revision = metaDataRepository.findById(revisionRestore.getId(), revisionRestore.getType());

        MetaData parent = metaDataRepository.findById(revision.getRevision().getParentId(),
                revisionRestore.getParentType());

        if (parent != null) {
            throw new IllegalArgumentException("Parent is not null");
        }
        String newId = revision.getRevision().getParentId();
        revision.getRevision().deTerminate(newId);
        metaDataRepository.update(revision);

        revision.restoreToLatest(newId, 0L, federatedUser.getUid(),
                revision.getRevision().getNumber(), revisionRestore.getParentType());
        //It might be that the revision is no longer valid as metaData configuration has changed
        revision = validate(revision);
        metaDataHook.prePost(revision, federatedUser);

        checkForDuplicateEntityId(revision, true);

        metaDataRepository.save(revision);

        LOG.info("Restored deleted revision {} with Id {} by {}", revisionRestore, revision.getId(), federatedUser
                .getUid());

        return revision;
    }

    private void checkForDuplicateEntityId(MetaData metaData, boolean isNew) {
        String entityid = (String) metaData.getData().get("entityid");
        List<Map> matchingEntities = uniqueEntityId(metaData.getType(), entityid);
        if ((isNew && !CollectionUtils.isEmpty(matchingEntities)) ||
                (matchingEntities.size() == 1 && !matchingEntities.get(0).get("_id").equals(metaData.getId())) ||
                matchingEntities.size() > 1) {
            throw new DuplicateEntityIdException(entityid);
        }
    }

    public MetaData restoreRevision(RevisionRestore revisionRestore, FederatedUser federatedUser)
            throws JsonProcessingException {
        MetaData revision = metaDataRepository.findById(revisionRestore.getId(), revisionRestore.getType());

        MetaData parent = metaDataRepository.findById(revision.getRevision().getParentId(),
                revisionRestore.getParentType());

        revision.restoreToLatest(parent.getId(), parent.getVersion(), federatedUser.getUid(),
                parent.getRevision().getNumber(), revisionRestore.getParentType());
        //It might be that the revision is no longer valid as metaData configuration has changed
        revision = validate(revision);
        metaDataRepository.update(revision);

        parent.revision(UUID.randomUUID().toString());
        metaDataRepository.save(parent);

        LOG.info("Restored revision {} with Id {} by {}", revisionRestore, revision.getId(), federatedUser.getUid());

        return revision;
    }

    public Map<String, List<Map>> autoCompleteEntities(String type, String query) {
        List<Map> suggestions = metaDataRepository.autoComplete(type, query);
        Map<String, List<Map>> results = new HashMap<>();
        results.put("suggestions", suggestions);
        if (suggestions.isEmpty() && entityTypesSuggestions.contains(type)) {
            List<Map> alternatives = new ArrayList<>();
            entityTypesSuggestions.stream().filter(s -> !s.equals(type))
                    .forEach(s -> alternatives.addAll(metaDataRepository.autoComplete(s, query)));
            results.put("alternatives", alternatives);
        }
        return results;
    }

    public List<Map> uniqueEntityId(String type, String entityId) {
        EntityType entityType = EntityType.fromType(type);
        List<Map> results;
        if (entityType.equals(EntityType.IDP) || entityType.equals(EntityType.STT)) {
            results = metaDataRepository.findByEntityId(entityType.getType(), entityId);
        } else if (entityType.equals(EntityType.RS)) {
            results = metaDataRepository.findByEntityId(entityType.getType(), entityId);
            results.addAll(metaDataRepository.findByEntityId(EntityType.RP.getType(), entityId));
        } else {
            results = metaDataRepository.findByEntityId(entityType.getType(), entityId);
            String otherType = entityType.equals(EntityType.RP) ? EntityType.SP.getType() : EntityType.RP.getType();
            results.addAll(metaDataRepository.findByEntityId(otherType, entityId));
            if (!entityType.equals(EntityType.SRAM)) {
                results.addAll(metaDataRepository.findByEntityId(EntityType.SRAM.getType(), entityId));
            }
            if (entityType.equals(EntityType.RP)) {
                results.addAll(metaDataRepository.findByEntityId(EntityType.RS.getType(), entityId));
            }
        }
        return results;
    }

    public List<Map> searchEntityByType(String type, Map<String, Object> properties, boolean nested) {
        List requestedAttributes = (List) properties.getOrDefault(REQUESTED_ATTRIBUTES, new
                ArrayList<String>());
        Boolean allAttributes = (Boolean) properties.getOrDefault(ALL_ATTRIBUTES, false);
        Boolean logicalOperatorIsAnd = (Boolean) properties.getOrDefault(LOGICAL_OPERATOR_IS_AND, true);
        properties.remove(REQUESTED_ATTRIBUTES);
        properties.remove(ALL_ATTRIBUTES);
        properties.remove(LOGICAL_OPERATOR_IS_AND);
        List<Map> search = metaDataRepository.search(type, properties, requestedAttributes, allAttributes,
                logicalOperatorIsAnd);
        return nested ? search.stream().map(m -> exporterService.nestMetaData(m, type)).collect(toList()) : search;
    }

    public List<MetaData> retrieveRawSearch(String type, String query) throws UnsupportedEncodingException {
        if (query.startsWith("%")) {
            query = URLDecoder.decode(query, "UTF-8");
        }
        return metaDataRepository.findRaw(type, query);
    }

    public List<MetaData> retrieveRecentActivity(Map<String, Object> properties) {
        if (properties == null) {
            properties = new HashMap<>();
        }
        Object limit = properties.getOrDefault("limit", 25);
        int max = 25;
        if (limit instanceof Integer) {
            max = (int) limit;
        }
        List<EntityType> types = ((List<String>) properties.getOrDefault("types",
                Arrays.asList(EntityType.IDP.getType(),
                        EntityType.SP.getType(),
                        EntityType.RP.getType(),
                        EntityType.RS.getType())))
                .stream().map(EntityType::fromType).collect(toList());
        return metaDataRepository.recentActivity(types, max);
    }

    public void createConnectWithoutInteraction(Map<String, String> connectionData, APIUser apiUser)
            throws JsonProcessingException {
        String idpEntityId = connectionData.get("idpId");
        MetaData idp = findByEntityId(idpEntityId, EntityType.IDP.getType());

        String spEntityId = connectionData.get("spId");
        String spType = connectionData.get("spType");
        MetaData sp = findByEntityId(spEntityId, spType);

        //We can connect automatically if the SP allows it or the IdP and SP share the institution ID
        String dashboardConnectType = (String) sp.metaDataFields().get(DASHBOARD_CONNECT_OPTION);
        boolean connectWithoutInteraction = StringUtils.hasText(dashboardConnectType) &&
                DashboardConnectOption.fromType(dashboardConnectType).connectWithoutInteraction();

        Object idpInstitutionId = idp.metaDataFields().get("coin:institution_id");
        Object spInstitutionId = sp.metaDataFields().get("coin:institution_id");
        boolean shareInstitutionId = idpInstitutionId != null && idpInstitutionId.equals(spInstitutionId) &&
                !"connect_with_interaction".equals(dashboardConnectType);
        if (!connectWithoutInteraction && !shareInstitutionId) {
            throw new EndpointNotAllowed(
                    String.format("SP %s does not allow an automatic connection with IdP %s. " +
                                    "SP dashboardConnectType: %s, idpInstitutionId: %s, spInstitutionId %s",
                            spEntityId, idpEntityId, dashboardConnectType, idpInstitutionId, spInstitutionId));
        }

        addAllowedEntity(sp, idpEntityId, connectionData, apiUser, false);
        addAllowedEntity(idp, spEntityId, connectionData, apiUser, true);

        databaseController.doPush(new PushOptions(true, true, false));
    }

    private void addAllowedEntity(MetaData metaData,
                                  String entityId,
                                  Map<String, String> connectionData,
                                  APIUser apiUser,
                                  boolean isIdp) throws JsonProcessingException {

        Map<String, Object> data = metaData.getData();
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>)
                data.getOrDefault("allowedEntities", new ArrayList<Map<String, String>>());
        boolean allowedAll = (boolean) data.getOrDefault("allowedall", true);
        boolean needsUpdate = false;

        String revisionNote = String.format("Connected %s on request of %s - %s via Dashboard.",
                entityId, connectionData.get("user"), connectionData.get("userUrn"));

        if (connectionData.containsKey("loaLevel") && isIdp) {
            List<Map<String, String>> stepupEntities = (List<Map<String, String>>)
                    data.getOrDefault("stepupEntities", new ArrayList<Map<String, String>>());
            Map<String, String> stepupEntity = new HashMap<>();
            stepupEntity.put("name", entityId);
            stepupEntity.put("level", connectionData.get("loaLevel"));
            stepupEntities.add(stepupEntity);
            data.put("stepupEntities", stepupEntities);
            data.put("revisionnote", revisionNote);
        }

        if (!allowedAll && allowedEntities.stream().noneMatch(allowedEntity ->
                allowedEntity.get("name").equals(entityId))) {
            allowedEntities.add(Collections.singletonMap("name", entityId));
            data.put("allowedEntities", allowedEntities);
            data.put("revisionnote", revisionNote);
            needsUpdate = true;
        }
        if (needsUpdate) {
            doPut(metaData, apiUser, false);
        }

    }

    private MetaData findByEntityId(String entityId, String type) {
        List<Map> searchResults = uniqueEntityId(type, entityId);
        if (CollectionUtils.isEmpty(searchResults)) {
            throw new ResourceNotFoundException(String.format("Type %s with entityId %s does not exists",
                    type, entityId));
        }
        return metaDataRepository.findById((String) searchResults.get(0).get("_id"), type);
    }

    public MetaData validate(MetaData metaData) throws JsonProcessingException {
        metaData = metaDataHook.preValidate(metaData);
        metaDataAutoConfiguration.validate(metaData.getData(), metaData.getType());
        return metaData;
    }

    public List<MetaData> findAllByType(String type) {
        return metaDataRepository.findAllByType(type);
    }

    private void checkNull(String type, String id, MetaData metaData) {
        if (metaData == null) {
            throw new ResourceNotFoundException(String.format("MetaData type %s with id %s does not exist", type, id));
        }
    }

    private void sanitizeExcludeFromPush(@RequestBody @Validated MetaData metaData, boolean excludeFromPushRequired) {
        if (excludeFromPushRequired) {
            Map metaDataFields = metaData.metaDataFields();
            Object val = metaDataFields.get("coin:exclude_from_push");
            if ("0".equals(val) || Boolean.FALSE == val) {
                metaDataFields.put("coin:exclude_from_push", true);
            }
        }
    }

    private void addNoValid(Map<String, List> results, String entityId, Exception e) {
        String msg = e instanceof ValidationException ?
                String.join(", ", ValidationException.class.cast(e).getAllMessages()) : e.getClass().getName();
        List notValid = results.computeIfAbsent("not_valid", s -> new ArrayList());
        Map<String, String> result = new HashMap<>();
        result.put("validationException", msg);
        result.put("entityId", entityId);
        notValid.add(result);
    }

    private MetaData importToMetaData(Map<String, Object> m, EntityType entityType) {
        Map<String, Object> data = metaDataAutoConfiguration.metaDataTemplate(entityType.getType());
        MetaData template = new MetaData(entityType.getType(), data);
        template.getData().putAll(m);
        template.getData().put("state", "prodaccepted");
        return template;
    }

    private MetaDataUpdate importToMetaDataUpdate(String id, EntityType entityType, Map<String, Object> m,
                                                  String feedUrl) {
        Map<String, Object> metaDataFields = Map.class.cast(m.get("metaDataFields"));
        Map<String, Object> pathUpdates = new HashMap<>();
        metaDataFields.forEach((k, v) -> pathUpdates.put("metaDataFields.".concat(k), v));
        pathUpdates.put("metadataurl", feedUrl);
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate(id,
                entityType.getType(),
                pathUpdates,
                Collections.emptyMap());
        return metaDataUpdate;
    }

    public void addDefaultSpData(Map<String, Object> innerJson) {
        innerJson.put("allowedall", true);
        innerJson.put("state", "testaccepted");
        innerJson.put("allowedEntities", new ArrayList<>());
    }

    public void deleteCollection(EntityType entityType) {
        this.metaDataRepository.getMongoTemplate().remove(new Query(),entityType.getType());
    }

}
