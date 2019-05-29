package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.api.APIUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.exception.DuplicateEntityIdException;
import manage.exception.ResourceNotFoundException;
import manage.format.Exporter;
import manage.format.Importer;
import manage.format.SaveURLResource;
import manage.hook.MetaDataHook;
import manage.model.EntityType;
import manage.model.Import;
import manage.model.MetaData;
import manage.model.MetaDataUpdate;
import manage.model.RevisionRestore;
import manage.model.ServiceProvider;
import manage.model.XML;
import manage.repository.MetaDataRepository;
import manage.shibboleth.FederatedUser;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static manage.api.Scope.TEST;
import static manage.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@RestController
@SuppressWarnings("unchecked")
public class MetaDataController {

    static final String REQUESTED_ATTRIBUTES = "REQUESTED_ATTRIBUTES";
    static final String ALL_ATTRIBUTES = "ALL_ATTRIBUTES";
    static final String LOGICAL_OPERATOR_IS_AND = "LOGICAL_OPERATOR_IS_AND";

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataController.class);

    private MetaDataRepository metaDataRepository;
    private MetaDataAutoConfiguration metaDataAutoConfiguration;
    private MetaDataHook metaDataHook;
    private Importer importer;
    private Exporter exporter;
    private Environment environment;

    @Autowired
    public MetaDataController(MetaDataRepository metaDataRepository,
                              MetaDataAutoConfiguration metaDataAutoConfiguration,
                              ResourceLoader resourceLoader,
                              MetaDataHook metaDataHook,
                              Environment environment,
                              @Value("${metadata_export_path}") String metadataExportPath,
                              @Value("${product.supported_languages}") String supportedLanguages) {
        this.metaDataRepository = metaDataRepository;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataHook = metaDataHook;
        List<String> languages = Stream.of(supportedLanguages.split(",")).map(String::trim).collect(toList());

        this.importer = new Importer(metaDataAutoConfiguration, languages);
        this.exporter = new Exporter(Clock.systemDefaultZone(), resourceLoader, metadataExportPath, languages);
        this.environment = environment;

    }

    @GetMapping("/client/template/{type}")
    public MetaData template(@PathVariable("type") String type) {
        Map<String, Object> data = metaDataAutoConfiguration.metaDataTemplate(type);
        return new MetaData(type, data);
    }

    @GetMapping({"/client/metadata/{type}/{id}", "/internal/metadata/{type}/{id}"})
    public MetaData get(@PathVariable("type") String type, @PathVariable("id") String id) {
        MetaData metaData = metaDataRepository.findById(id, type);
        checkNull(type, id, metaData);
        return metaDataHook.postGet(metaData);
    }

    private void checkNull(@PathVariable("type") String type, @PathVariable("id") String id, MetaData metaData) {
        if (metaData == null) {
            throw new ResourceNotFoundException(String.format("MetaData type %s with id %s does not exist", type, id));
        }
    }

    @GetMapping("/client/metadata/configuration")
    public List<Map<String, Object>> configuration() {
        return metaDataAutoConfiguration.schemaRepresentations();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/client/metadata")
    public MetaData post(@Validated @RequestBody MetaData metaData, FederatedUser federatedUser) throws
            JsonProcessingException {
        return doPost(metaData, federatedUser.getUid(), false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/includeInPush/{type}/{id}")
    public MetaData includeInPush(@PathVariable("type") String type, @PathVariable("id") String id,
                                  FederatedUser federatedUser) throws JsonProcessingException {
        MetaData metaData = this.get(type, id);
        Map metaDataFields = metaData.metaDataFields();
        metaDataFields.remove("coin:exclude_from_push");
        return doPut(metaData, federatedUser.getUid(), false);
    }


    @PreAuthorize("hasRole('WRITE')")
    @PostMapping("/internal/metadata")
    public MetaData postInternal(@Validated @RequestBody MetaData metaData, APIUser apiUser) throws
            JsonProcessingException {
        return doPost(metaData, apiUser.getName(), !apiUser.getScopes().contains(TEST));
    }

    @PreAuthorize("hasRole('WRITE')")
    @PostMapping("/internal/new-sp")
    public MetaData newSP(@Validated @RequestBody XML container, APIUser apiUser) throws
            IOException, XMLStreamException {
        Map<String, Object> innerJson = this.importer.importXML(new ByteArrayResource(container.getXml()
                .getBytes()), EntityType.SP, Optional.empty());
        String entityId = String.class.cast(innerJson.get("entityid"));
        List<Map> result = metaDataRepository.search(EntityType.SP.getType(), singletonMap("entityid",
                entityId), emptyList(), false, true);

        if (!CollectionUtils.isEmpty(result)) {
            throw new DuplicateEntityIdException(entityId);
        }

        addDefaultSpData(innerJson);
        MetaData metaData = new MetaData(EntityType.SP.getType(), innerJson);

        return doPost(metaData, apiUser.getName(), !apiUser.getScopes().contains(TEST));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/client/delete/feed")
    public Map<String, Integer> deleteFeed() {
        int deleted = this.metaDataRepository.deleteAllImportedServiceProviders();
        return Collections.singletonMap("deleted", deleted);
    }

    @GetMapping(value = "/client/count/feed")
    public Map<String, Long> countFeed() {
        long count = this.metaDataRepository.countAllImportedServiceProviders();
        return Collections.singletonMap("count", count);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/feed")
    public Map<String, List> importFeed(@Validated @RequestBody Import importRequest) {
        try {
            Map<String, ServiceProvider> serviceProviderMap =
                    metaDataRepository.allServiceProviderEntityIds().stream()
                            .map(ServiceProvider::new)
                            .collect(Collectors.toMap(sp -> sp.getEntityId(), sp -> sp));
            String feedUrl = importRequest.getUrl();
            Resource resource = new SaveURLResource(new URL(feedUrl), environment.acceptsProfiles("dev"));

            List<Map<String, Object>> allImports = this.importer.importFeed(resource);
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
                                    this.importToMetaDataUpdate(existingServiceProvider.getId(), entityType, sp, feedUrl);
                            Optional<MetaData> metaData = this.doMergeUpdate(metaDataUpdate, "edugain-import", false);
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
                        MetaData metaData = this.importToMetaData(sp, entityType);
                        MetaData persistedMetaData = this.doPost(metaData, "edugain-import", false);
                        List imported = results.computeIfAbsent("imported", s -> new ArrayList());
                        imported.add(new ServiceProvider(persistedMetaData.getId(), entityId, false, false, null));
                    } catch (JsonProcessingException | ValidationException e) {
                        addNoValid(results, entityId, e);
                    }
                }
            });
            List<ServiceProvider> notInFeedAnymore = serviceProviderMap.values().stream()
                    .filter(sp -> sp.isImportedFromEduGain() &&
                            !imports.stream().anyMatch(map -> sp.getEntityId().equals(map.get("entityid"))))
                    .collect(toList());
            notInFeedAnymore.forEach(sp -> this.doRemove(entityType.getType(), sp.getId(), "edugain-import"));

            List deleted = results.computeIfAbsent("deleted", s -> new ArrayList());
            deleted.addAll(notInFeedAnymore.stream().map(sp -> sp.getEntityId()).collect(toList()));

            results.put("total", Collections.singletonList(imports.size()));

            return results;
        } catch (IOException | XMLStreamException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
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
        MetaData template = this.template(entityType.getType());
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
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate(id, entityType.getType(), pathUpdates, Collections.emptyMap());
        return metaDataUpdate;
    }

    private void addDefaultSpData(Map<String, Object> innerJson) {
        innerJson.put("allowedall", true);
        innerJson.put("state", "testaccepted");
        innerJson.put("allowedEntities", new ArrayList<>());
    }

    @PreAuthorize("hasRole('WRITE')")
    @PostMapping("/internal/update-sp/{id}/{version}")
    public MetaData updateSP(@PathVariable("id") String id,
                             @PathVariable("version") Long version,
                             @Validated @RequestBody XML container,
                             APIUser apiUser) throws IOException, XMLStreamException {
        MetaData metaData = this.get(EntityType.SP.getType(), id);
        Map<String, Object> innerJson = this.importer.importXML(new ByteArrayResource(container.getXml()
                .getBytes()), EntityType.SP, Optional.empty());

        addDefaultSpData(innerJson);

        metaData.setData(innerJson);
        metaData.setVersion(version);

        return doPut(metaData, apiUser.getName(), !apiUser.getScopes().contains(TEST));
    }

    @GetMapping("/internal/sp-metadata/{id}")
    public String exportXml(@PathVariable("id") String id) throws IOException {
        MetaData metaData = this.get(EntityType.SP.getType(), id);
        return exporter.exportToXml(metaData);
    }


    private MetaData doPost(@Validated @RequestBody MetaData metaData, String uid, boolean excludeFromPushRequired) throws JsonProcessingException {
        sanitizeExcludeFromPush(metaData, excludeFromPushRequired);
        metaData = metaDataHook.prePost(metaData);

        metaData = validate(metaData);
        Long eid = metaDataRepository.incrementEid();
        metaData.initial(UUID.randomUUID().toString(), uid, eid);

        LOG.info("Saving new metaData {} by {}", metaData.getId(), uid);

        metaDataRepository.save(metaData);

        return this.get(metaData.getType(), metaData.getId());
    }

    private void sanitizeExcludeFromPush(@RequestBody @Validated MetaData metaData, boolean excludeFromPushRequired) {
        Map metaDataFields = metaData.metaDataFields();
        if (excludeFromPushRequired && !"1".equals(metaDataFields.get("coin:exclude_from_push"))) {
            metaDataFields.put("coin:exclude_from_push", "1");
        }
    }

    @PreAuthorize("hasRole('READ')")
    @PostMapping("/internal/validate/metadata")
    public ResponseEntity<Object> validateMetaData(@Validated @RequestBody MetaData metaData) throws
            JsonProcessingException {
        validate(metaData);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/client/metadata/{type}/{id}")
    public boolean remove(@PathVariable("type") String type, @PathVariable("id") String id, FederatedUser user) {
        return doRemove(type, id, user.getUid());
    }

    @PreAuthorize("hasRole('WRITE')")
    @DeleteMapping("/internal/metadata/{type}/{id}")
    public boolean removeInternal(@PathVariable("type") String type, @PathVariable("id") String id, APIUser apiUser) {
        return doRemove(type, id, apiUser.getName());
    }

    private boolean doRemove(@PathVariable("type") String type, @PathVariable("id") String id, String uid) {
        MetaData current = metaDataRepository.findById(id, type);
        checkNull(type, id, current);
        current = metaDataHook.preDelete(current);
        metaDataRepository.remove(current);

        LOG.info("Deleted metaData {} by {}", current.getId(), uid);

        current.terminate(UUID.randomUUID().toString());
        metaDataRepository.save(current);
        return true;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/metadata")
    @Transactional
    public MetaData put(@Validated @RequestBody MetaData metaData, FederatedUser federatedUser) throws
            JsonProcessingException {
        return doPut(metaData, federatedUser.getUid(), false);
    }

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("/internal/metadata")
    @Transactional
    public MetaData putInternal(@Validated @RequestBody MetaData metaData, APIUser apiUser) throws
            JsonProcessingException {
        return doPut(metaData, apiUser.getName(), !apiUser.getScopes().contains(TEST));
    }

    private MetaData doPut(@Validated @RequestBody MetaData metaData, String updatedBy, boolean excludeFromPushRequired) throws JsonProcessingException {
        sanitizeExcludeFromPush(metaData, excludeFromPushRequired);
        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());
        checkNull(metaData.getType(), id, previous);

        metaData = metaDataHook.prePut(previous, metaData);
        metaData = validate(metaData);

        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest(updatedBy);
        metaDataRepository.update(metaData);

        LOG.info("Updated metaData {} by {}", metaData.getId(), updatedBy);

        return this.get(metaData.getType(), metaData.getId());
    }

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("internal/merge")
    @Transactional
    public MetaData update(@Validated @RequestBody MetaDataUpdate metaDataUpdate, APIUser apiUser) throws
            JsonProcessingException {
        String name = apiUser.getName();
        return doMergeUpdate(metaDataUpdate, name, true).get();
    }

    private Optional<MetaData> doMergeUpdate(MetaDataUpdate metaDataUpdate, String name, boolean forceNewRevision)
            throws JsonProcessingException {
        String id = metaDataUpdate.getId();
        MetaData previous = metaDataRepository.findById(id, metaDataUpdate.getType());
        checkNull(metaDataUpdate.getType(), id, previous);

        previous.revision(UUID.randomUUID().toString());

        MetaData metaData = metaDataRepository.findById(id, metaDataUpdate.getType());
        metaData.promoteToLatest(name);
        metaData.merge(metaDataUpdate);

        if (!CollectionUtils.isEmpty(metaDataUpdate.getExternalReferenceData())) {
            metaData.getData().putAll(metaDataUpdate.getExternalReferenceData());
        }
        metaData = metaDataHook.prePut(previous, metaData);
        metaData = validate(metaData);
        //Only save and update if there are changes
        boolean somethingChanged = !metaData.getData().equals(previous.getData());

        if (somethingChanged || forceNewRevision) {


            metaDataRepository.save(previous);
            metaDataRepository.update(metaData);

            LOG.info("Merging new metaData {} by {}", metaData.getId(), name);

            return Optional.of(this.get(metaData.getType(), metaData.getId()));
        } else {
            return Optional.empty();
        }
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/restoreDeleted")
    @Transactional
    public MetaData restoreDeleted(@Validated @RequestBody RevisionRestore revisionRestore,
                                   FederatedUser federatedUser) throws JsonProcessingException {
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


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/restoreRevision")
    @Transactional
    public MetaData restoreRevision(@Validated @RequestBody RevisionRestore revisionRestore,
                                    FederatedUser federatedUser) throws JsonProcessingException {
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

    @GetMapping("/client/revisions/{type}/{parentId}")
    public List<MetaData> revisions(@PathVariable("type") String type, @PathVariable("parentId") String parentId) {
        return metaDataRepository.revisions(type.concat(REVISION_POSTFIX), parentId);
    }

    @GetMapping("/client/autocomplete/{type}")
    public List<Map> autoCompleteEntities(@PathVariable("type") String type, @RequestParam("query") String query) {
        return metaDataRepository.autoComplete(type, query);
    }

    @GetMapping("/client/whiteListing/{type}")
    public List<Map> whiteListing(@PathVariable("type") String type,
                                  @RequestParam(value = "state") String state) {
        return metaDataRepository.whiteListing(type, state);
    }

    @PostMapping({"/client/search/{type}", "/internal/search/{type}"})
    public List<Map> searchEntities(@PathVariable("type") String type,
                                    @RequestBody Map<String, Object> properties,
                                    @RequestParam(required = false, defaultValue = "false") boolean nested) {
        List requestedAttributes = List.class.cast(properties.getOrDefault(REQUESTED_ATTRIBUTES, new
                ArrayList<String>()));
        Boolean allAttributes = Boolean.class.cast(properties.getOrDefault(ALL_ATTRIBUTES, false));
        Boolean logicalOperatorIsAnd = Boolean.class.cast(properties.getOrDefault(LOGICAL_OPERATOR_IS_AND, true));
        properties.remove(REQUESTED_ATTRIBUTES);
        properties.remove(ALL_ATTRIBUTES);
        properties.remove(LOGICAL_OPERATOR_IS_AND);
        List<Map> search = metaDataRepository.search(type, properties, requestedAttributes, allAttributes,
                logicalOperatorIsAnd);
        return nested ? search.stream().map(m -> exporter.nestMetaData(m, type)).collect(toList()) : search;
    }

    @GetMapping({"/client/rawSearch/{type}", "/internal/rawSearch/{type}"})
    public List<MetaData> rawSearch(@PathVariable("type") String type, @RequestParam("query") String query) throws
            UnsupportedEncodingException {
        if (query.startsWith("%")) {
            query = URLDecoder.decode(query, "UTF-8");
        }
        return metaDataRepository.findRaw(type, query);
    }

    private MetaData validate(MetaData metaData) throws JsonProcessingException {
        metaData = metaDataHook.preValidate(metaData);
        metaDataAutoConfiguration.validate(metaData.getData(), metaData.getType());
        return metaData;
    }

}
