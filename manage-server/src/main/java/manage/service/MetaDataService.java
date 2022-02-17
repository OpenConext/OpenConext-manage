package manage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import manage.api.APIUser;
import manage.api.AbstractUser;
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
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

@Service
public class MetaDataService {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataService.class);

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
            Resource resource = new SaveURLResource(new URL(feedUrl), environment.acceptsProfiles(Profiles.of("dev")));

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
                                    "edugain-import",
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
                        MetaData persistedMetaData = doPost(metaData, "edugain-import", false);
                        List imported = results.computeIfAbsent("imported", s -> new ArrayList());
                        imported.add(new ServiceProvider(persistedMetaData.getId(), entityId, false, false, null));
                    } catch (JsonProcessingException | ValidationException e) {
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
                    "edugain-import",
                    "Removed from eduGain feed"));

            List deleted = results.computeIfAbsent("deleted", s -> new ArrayList());
            deleted.addAll(notInFeedAnymore.stream().map(ServiceProvider::getEntityId).collect(toList()));

            results.put("total", Collections.singletonList(imports.size()));

            return results;
        } catch (IOException | XMLStreamException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
        }
    }

    public MetaData doPost(@Validated MetaData metaData, String uid, boolean excludeFromPushRequired)
            throws JsonProcessingException {
        String entityid = (String) metaData.getData().get("entityid");
        List<Map> result = uniqueEntityId(metaData.getType(), entityid);
        if (!CollectionUtils.isEmpty(result)) {
            throw new DuplicateEntityIdException(entityid);
        }

        sanitizeExcludeFromPush(metaData, excludeFromPushRequired);
        metaData = metaDataHook.prePost(metaData);

        metaData = validate(metaData);
        Long eid = metaDataRepository.incrementEid();
        metaData.initial(UUID.randomUUID().toString(), uid, eid);

        LOG.info("Saving new metaData {} by {}", metaData.getId(), uid);

        metaDataRepository.save(metaData);

        return getMetaDataAndValidate(metaData.getType(), metaData.getId());
    }

    public boolean doRemove(String type, String id, String uid, String revisionNote) {
        MetaData current = metaDataRepository.findById(id, type);
        checkNull(type, id, current);
        current = metaDataHook.preDelete(current);
        metaDataRepository.remove(current);

        LOG.info("Deleted metaData {} by {}", current.getId(), uid);

        current.revision(UUID.randomUUID().toString());
        metaDataRepository.save(current);

        current.terminate(UUID.randomUUID().toString(), revisionNote, uid);
        metaDataRepository.save(current);
        return true;
    }

    public MetaData doPut(@Validated MetaData metaData, String updatedBy, boolean excludeFromPushRequired)
            throws JsonProcessingException {
        String entityid = (String) metaData.getData().get("entityid");
        List<Map> result = uniqueEntityId(metaData.getType(), entityid);
        if (result.size() > 1) {
            throw new DuplicateEntityIdException(entityid);
        }

        sanitizeExcludeFromPush(metaData, excludeFromPushRequired);
        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());
        checkNull(metaData.getType(), id, previous);

        metaData = metaDataHook.prePut(previous, metaData);
        metaData = validate(metaData);

        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest(updatedBy, (String) metaData.getData().get("revisionnote"));
        metaDataRepository.update(metaData);

        LOG.info("Updated metaData {} by {}", metaData.getId(), updatedBy);

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

    public Optional<MetaData> doMergeUpdate(MetaDataUpdate metaDataUpdate,
                                            String name,
                                            String revisionNote,
                                            boolean forceNewRevision) throws JsonProcessingException {
        String id = metaDataUpdate.getId();
        MetaData previous = metaDataRepository.findById(id, metaDataUpdate.getType());
        checkNull(metaDataUpdate.getType(), id, previous);

        previous.revision(UUID.randomUUID().toString());

        MetaData metaData = metaDataRepository.findById(id, metaDataUpdate.getType());
        metaData.promoteToLatest(name, revisionNote);
        metaData.merge(metaDataUpdate);

        if (!CollectionUtils.isEmpty(metaDataUpdate.getExternalReferenceData())) {
            metaData.getData().putAll(metaDataUpdate.getExternalReferenceData());
        }
        if (!name.equals("edugain-import")) {
            metaData = metaDataHook.prePut(previous, metaData);
        }
        metaData = validate(metaData);
        //Only save and update if there are changes
        boolean somethingChanged = !metaData.metaDataFields().equals(previous.metaDataFields());

        if (somethingChanged || forceNewRevision) {
            metaDataRepository.save(previous);
            metaDataRepository.update(metaData);

            LOG.info("Merging new metaData {} by {}", metaData.getId(), name);

            return Optional.of(getMetaDataAndValidate(metaData.getType(), metaData.getId()));
        } else {
            return Optional.empty();
        }
    }

    public MetaDataChangeRequest doChangeRequest(MetaDataChangeRequest metaDataChangeRequest, AbstractUser user) throws JsonProcessingException {
        String id = metaDataChangeRequest.getMetaDataId();
        MetaData metaData = metaDataRepository.findById(id, metaDataChangeRequest.getType());
        checkNull(metaDataChangeRequest.getType(), id, metaData);

        //fail fast if there are validation errors
        metaData.merge(metaDataChangeRequest);
        validate(metaData);

        metaDataChangeRequest.getAuditData().put("userName", user.getName());
        metaDataChangeRequest.getAuditData().put("apiUser", user.isAPIUser());
        metaDataChangeRequest.setMetaDataSummary(metaData.summary());

        return metaDataRepository.save(metaDataChangeRequest);
    }


    public MetaData doAcceptChangeRequest(ChangeRequest changeRequest, FederatedUser user) {
        //TODO
        return null;
    }

    public MetaData doRejectChangeRequest(ChangeRequest changeRequest, FederatedUser user) {
        //TODO
        return null;
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
        metaDataRepository.save(revision);

        LOG.info("Restored deleted revision {} with Id {} by {}", revisionRestore, revision.getId(), federatedUser
                .getUid());

        return revision;
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

        addAllowedEntity(sp, idpEntityId, connectionData, apiUser);
        addAllowedEntity(idp, spEntityId, connectionData, apiUser);

        databaseController.doPush();
    }

    private void addAllowedEntity(MetaData metaData, String entityId, Map<String, String> connectionData,
                                  APIUser apiUser) throws JsonProcessingException {

        Map<String, Object> data = metaData.getData();
        List<Map<String, String>> allowedEntities = (List<Map<String, String>>)
                data.getOrDefault("allowedEntities", new ArrayList<Map<String, String>>());
        boolean allowedAll = (boolean) data.getOrDefault("allowedall", true);

        if (!allowedAll && allowedEntities.stream().noneMatch(allowedEntity ->
                allowedEntity.get("name").equals(entityId))) {
            allowedEntities.add(Collections.singletonMap("name", entityId));
            data.put("allowedEntities", allowedEntities);
            String revisionNote = String.format("Connected %s on request of %s - %s via Dashboard.",
                    entityId, connectionData.get("user"), connectionData.get("userUrn"));
            data.put("revisionnote", revisionNote);
            doPut(metaData, apiUser.getName(), false);
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

    private void checkNull(String type, String id, MetaData metaData) {
        if (metaData == null) {
            throw new ResourceNotFoundException(String.format("MetaData type %s with id %s does not exist", type, id));
        }
    }

    private void sanitizeExcludeFromPush(@RequestBody @Validated MetaData metaData, boolean excludeFromPushRequired) {
        Map metaDataFields = metaData.metaDataFields();
        Object val = metaDataFields.get("coin:exclude_from_push");
        if (excludeFromPushRequired && ("0".equals(val) || Boolean.FALSE == val)) {
            metaDataFields.put("coin:exclude_from_push", true);
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

}
