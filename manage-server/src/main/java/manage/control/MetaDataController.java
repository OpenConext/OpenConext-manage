package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.api.APIUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.exception.DuplicateEntityIdException;
import manage.exception.ResourceNotFoundException;
import manage.format.Exporter;
import manage.format.Importer;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.UrlResource;
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

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static manage.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@RestController
@SuppressWarnings("unchecked")
public class MetaDataController {

    public static final String REQUESTED_ATTRIBUTES = "REQUESTED_ATTRIBUTES";
    public static final String ALL_ATTRIBUTES = "ALL_ATTRIBUTES";
    public static final String LOGICAL_OPERATOR_IS_AND = "LOGICAL_OPERATOR_IS_AND";

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataController.class);

    private MetaDataRepository metaDataRepository;
    private MetaDataAutoConfiguration metaDataAutoConfiguration;
    private MetaDataHook metaDataHook;
    private Importer importer;
    private Exporter exporter;

    @Autowired
    public MetaDataController(MetaDataRepository metaDataRepository,
                              MetaDataAutoConfiguration metaDataAutoConfiguration,
                              ResourceLoader resourceLoader,
                              MetaDataHook metaDataHook,
                              @Value("${metadata_export_path}") String metadataExportPath) {
        this.metaDataRepository = metaDataRepository;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataHook = metaDataHook;
        this.importer = new Importer(metaDataAutoConfiguration);
        this.exporter = new Exporter(Clock.systemDefaultZone(), resourceLoader, metadataExportPath);

    }

    @GetMapping("/client/template/{type}")
    public MetaData template(@PathVariable("type") String type) {
        Map<String, Object> data = metaDataAutoConfiguration.metaDataTemplate(type);
        return new MetaData(type, data);
    }

    @GetMapping({"/client/metadata/{type}/{id}", "/internal/metadata/{type}/{id}"})
    public MetaData get(@PathVariable("type") String type, @PathVariable("id") String id) {
        MetaData metaData = metaDataRepository.findById(id, type);
        if (metaData == null) {
            throw new ResourceNotFoundException(String.format("MetaData type %s with id %s does not exist", type, id));
        }
        return metaData;
    }

    @GetMapping("/client/metadata/configuration")
    public List<Map<String, Object>> configuration() {
        return metaDataAutoConfiguration.schemaRepresentations();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/client/metadata")
    public MetaData post(@Validated @RequestBody MetaData metaData, FederatedUser federatedUser) throws
        JsonProcessingException {
        return doPost(metaData, federatedUser.getUid());
    }

    @PreAuthorize("hasRole('WRITE')")
    @PostMapping("/internal/metadata")
    public MetaData postInternal(@Validated @RequestBody MetaData metaData, APIUser apiUser) throws
        JsonProcessingException {
        return doPost(metaData, apiUser.getName());
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

        return doPost(metaData, apiUser.getName());
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
            Resource resource = new UrlResource(new URL(feedUrl));

            List<Map<String, Object>> allImports = this.importer.importFeed(resource);
            List<Map<String, Object>> imports =
                allImports.stream().filter(m -> !m.isEmpty()).collect(Collectors.toList());

            Map<String, List> results = new HashMap<>();
            EntityType entityType = EntityType.SP;
            imports.forEach(sp -> {
                String entityId = (String) sp.get("entityid");
                sp.put("metadataurl", feedUrl);
                Map metaDataFields = Map.class.cast(sp.get("metaDataFields"));
                metaDataFields.put("coin:imported_from_edugain", "1");
                metaDataFields.put("coin:interfed_source", "eduGAIN");

                ServiceProvider existingServiceProvider = serviceProviderMap.get(entityId);
                if (existingServiceProvider != null) {
                    if (existingServiceProvider.isImportedFromEduGain()) {
                        try {
                            MetaDataUpdate metaDataUpdate =
                                this.importToMetaDataUpdate(existingServiceProvider.getId(), entityType, sp);
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
                        MetaData persistedMetaData = this.doPost(metaData, "edugain-import");
                        List imported = results.computeIfAbsent("imported", s -> new ArrayList());
                        imported.add(new ServiceProvider(persistedMetaData.getId(), entityId, false, null));
                    } catch (JsonProcessingException | ValidationException e) {
                        addNoValid(results, entityId, e);
                    }
                }
            });
            List<ServiceProvider> notInFeedAnymore = serviceProviderMap.values().stream()
                .filter(sp -> sp.isImportedFromEduGain() &&
                    !imports.stream().anyMatch(map -> sp.getEntityId().equals(map.get("entityid"))))
                .collect(Collectors.toList());
            notInFeedAnymore.forEach(sp -> this.doRemove(entityType.getType(),sp.getId(),"edugain-import"));

            List deleted = results.computeIfAbsent("deleted", s -> new ArrayList());
            deleted.addAll(notInFeedAnymore.stream().map(sp -> sp.getEntityId()).collect(Collectors.toList()));

            results.put("total", Collections.singletonList(imports.size()));

            return results;
        } catch (IOException | XMLStreamException e) {
            return singletonMap("errors", singletonList(e.toString()));
        }
    }

    private void addNoValid(Map<String, List> results, String entityId, Exception e) {
        String msg = e instanceof ValidationException ?
            String.join(", ", ValidationException.class.cast(e).getAllMessages()) : e.toString();
        List notValid = results.computeIfAbsent("not_valid", s -> new ArrayList());
        Map<String, String> result = new HashMap<>();
        result.put("validationException", msg);
        result.put("entityId", entityId);
        notValid.add(result);
    }

    private MetaData importToMetaData(Map<String, Object> m, EntityType entityType) {
        Map.class.cast(m.get("metaDataFields")).put("coin:imported_from_edugain", "1");
        MetaData template = this.template(entityType.getType());
        template.getData().putAll(m);
        return template;
    }

    private MetaDataUpdate importToMetaDataUpdate(String id, EntityType entityType, Map<String, Object> m) {
        Map<String, String> metaDataFields = Map.class.cast(m.get("metaDataFields"));
        Map<String, Object> pathUpdates = new HashMap<>();
        metaDataFields.forEach((k, v) -> pathUpdates.put("metaDataFields.".concat(k), v));
        MetaDataUpdate metaDataUpdate = new MetaDataUpdate(id, entityType.getType(), pathUpdates);
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

        return doPut(metaData, apiUser.getName());
    }

    @GetMapping("/internal/sp-metadata/{id}")
    public String exportXml(@PathVariable("id") String id) throws IOException, XMLStreamException {
        MetaData metaData = this.get(EntityType.SP.getType(), id);
        return exporter.exportToXml(metaData);
    }


    private MetaData doPost(@Validated @RequestBody MetaData metaData, String uid) throws JsonProcessingException {
        validate(metaData);
        metaData = metaDataHook.prePost(metaData);
        Long eid = metaDataRepository.incrementEid();
        metaData.initial(UUID.randomUUID().toString(), uid, eid);

        LOG.info("Saving new metaData {} by {}", metaData.getId(), uid);

        return metaDataRepository.save(metaData);
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
        String uid = user.getUid();

        return doRemove(type, id, uid);
    }

    private boolean doRemove(@PathVariable("type") String type, @PathVariable("id") String id, String uid) {
        MetaData current = metaDataRepository.findById(id, type);
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
        return doPut(metaData, federatedUser.getUid());
    }

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("/internal/metadata")
    @Transactional
    public MetaData putInternal(@Validated @RequestBody MetaData metaData, APIUser apiUser) throws
        JsonProcessingException {
        return doPut(metaData, apiUser.getName());
    }

    private MetaData doPut(@Validated @RequestBody MetaData metaData, String updatedBy) throws JsonProcessingException {
        validate(metaData);
        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());

        metaData = metaDataHook.prePut(previous, metaData);

        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest(updatedBy);
        metaDataRepository.update(metaData);

        LOG.info("Updated metaData {} by {}", metaData.getId(), updatedBy);

        return metaData;
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
        previous.revision(UUID.randomUUID().toString());

        MetaData metaData = metaDataRepository.findById(id, metaDataUpdate.getType());
        metaData.promoteToLatest(name);
        metaData.merge(metaDataUpdate);

        //Only save and update if there are changes
        boolean somethingChanged = !metaData.getData().equals(previous.getData());

        if (somethingChanged || forceNewRevision) {
            validate(metaData);

            metaDataRepository.save(previous);
            metaDataRepository.update(metaData);

            LOG.info("Merging new metaData {} by {}", metaData.getId(), name);

            return Optional.of(metaData);
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
        validate(revision);
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
        validate(revision);
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
    public List<Map> whiteListing(@PathVariable("type") String type) {
        return metaDataRepository.whiteListing(type);
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
        return nested ? search.stream().map(m -> exporter.nestMetaData(m, type)).collect(Collectors.toList()) : search;
    }

    @GetMapping({"/client/rawSearch/{type}", "/internal/rawSearch/{type}"})
    public List<MetaData> rawSearch(@PathVariable("type") String type, @RequestParam("query") String query) throws
        UnsupportedEncodingException {
        if (query.startsWith("%")) {
            query = URLDecoder.decode(query, "UTF-8");
        }
        return metaDataRepository.findRaw(type, query);
    }

    private void validate(MetaData metaData) throws JsonProcessingException {
        String json = metaDataAutoConfiguration.getObjectMapper().writeValueAsString(metaData.getData());
        metaDataAutoConfiguration.validate(json, metaData.getType());
    }

}
