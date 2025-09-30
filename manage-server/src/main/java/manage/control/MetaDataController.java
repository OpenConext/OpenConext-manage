package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.result.DeleteResult;
import manage.api.APIUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.*;
import manage.repository.MetaDataRepository;
import manage.service.ExporterService;
import manage.service.ImporterService;
import manage.service.MetaDataService;
import manage.shibboleth.FederatedUser;
import manage.web.ScopeEnforcer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static manage.api.Scope.TEST;
import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;
import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@RestController
@SuppressWarnings("unchecked")
public class MetaDataController {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataController.class);

    private final MetaDataRepository metaDataRepository;

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    private final MetaDataService metaDataService;

    private final ExporterService exporterService;

    private final ImporterService importerService;

    public MetaDataController(MetaDataRepository metaDataRepository,
                              MetaDataAutoConfiguration metaDataAutoConfiguration,
                              ExporterService exporterService,
                              ImporterService importerService,
                              MetaDataService metaDataService) {

        this.metaDataRepository = metaDataRepository;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.exporterService = exporterService;
        this.importerService = importerService;
        this.metaDataService = metaDataService;

    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/template/{type}")
    public MetaData template(@PathVariable("type") String type) {
        Map<String, Object> data = metaDataAutoConfiguration.metaDataTemplate(type);
        return new MetaData(type, data);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @GetMapping({"/client/metadata/{type}/{id}", "/internal/metadata/{type}/{id}"})
    public MetaData get(@PathVariable("type") String type, @PathVariable("id") String id) {
        return metaDataService.getMetaDataAndValidate(type, id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/metadata/configuration")
    public List<Map<String, Object>> configuration() {
        return metaDataAutoConfiguration.schemaRepresentations();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/client/metadata")
    public MetaData post(@Validated @RequestBody MetaData metaData, FederatedUser federatedUser)
        throws JsonProcessingException {

        return metaDataService.doPost(metaData, federatedUser, false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/includeInPush/{type}/{id}")
    public MetaData includeInPush(@PathVariable("type") String type,
                                  @PathVariable("id") String id,
                                  FederatedUser federatedUser) throws JsonProcessingException {

        MetaData metaData = this.get(type, id);
        Map metaDataFields = metaData.metaDataFields();
        metaDataFields.remove("coin:exclude_from_push");

        return metaDataService.doPut(metaData, federatedUser, false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/metadata/stats")
    public List<StatsEntry> stats(FederatedUser user) {

        return metaDataRepository.stats();
    }

    @PreAuthorize("hasAnyRole('WRITE_SP', 'WRITE_IDP', 'SYSTEM')")
    @PostMapping("/internal/metadata")
    public MetaData postInternal(@Validated @RequestBody MetaData metaData, APIUser apiUser) {
        ScopeEnforcer.enforceWriteScope(apiUser, EntityType.fromType(metaData.getType()));
        return metaDataService.doPost(metaData, apiUser, !apiUser.getScopes().contains(TEST));
    }

    @PreAuthorize("hasRole('WRITE_SP')")
    @PostMapping("/internal/new-sp")
    public MetaData newSP(@Validated @RequestBody XML container, APIUser apiUser)
        throws IOException, XMLStreamException {
        Map<String, Object> innerJson = importerService.importXML(new ByteArrayResource(container.getXml()
            .getBytes()), EntityType.SP, Optional.empty());

        metaDataService.addDefaultSpData(innerJson);
        MetaData metaData = new MetaData(EntityType.SP.getType(), innerJson);

        return metaDataService.doPost(metaData, apiUser, !apiUser.getScopes().contains(TEST));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/client/delete/feed")
    public Map<String, Long> deleteFeed() {
        long deleted = this.metaDataRepository.deleteAllImportedServiceProviders();
        return Collections.singletonMap("deleted", deleted);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/client/count/feed")
    public Map<String, Long> countFeed() {
        long count = this.metaDataRepository.countAllImportedServiceProviders();
        return Collections.singletonMap("count", count);
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/feed")
    public Map<String, List> importFeed(@Validated @RequestBody Import importRequest) {
        return metaDataService.importFeed(importRequest);
    }

    @PreAuthorize("hasRole('WRITE_SP')")
    @PostMapping("/internal/update-sp/{id}/{version}")
    public MetaData updateSP(@PathVariable("id") String id,
                             @PathVariable("version") Long version,
                             @Validated @RequestBody XML container,
                             APIUser apiUser) throws IOException, XMLStreamException {

        MetaData metaData = metaDataService.getMetaDataAndValidate(EntityType.SP.getType(), id);
        Map<String, Object> innerJson = importerService.importXML(new ByteArrayResource(container.getXml()
            .getBytes()), EntityType.SP, Optional.empty());

        metaDataService.addDefaultSpData(innerJson);

        metaData.setData(innerJson);
        metaData.setVersion(version);

        return metaDataService.doPut(metaData, apiUser, !apiUser.getScopes().contains(TEST));
    }

    @PreAuthorize("hasRole('READ')")
    @GetMapping("/internal/sp-metadata/{id}")
    public String exportXml(@PathVariable("id") String id) throws IOException {
        MetaData metaData = metaDataService.getMetaDataAndValidate(EntityType.SP.getType(), id);
        return exporterService.exportToXml(metaData);
    }

    @PreAuthorize("hasRole('READ')")
    @GetMapping(value = "/internal/xml/metadata/{type}/{id}", produces = "text/xml")
    public String exportMetadataXml(@PathVariable("type") String type,
                                    @PathVariable("id") String id) throws IOException {

        MetaData metaData = metaDataService.getMetaDataAndValidate(EntityType.fromType(type).getType(), id);
        return exporterService.exportToXml(metaData);
    }

    @PreAuthorize("hasRole('READ')")
    @PostMapping("/internal/validate/metadata")
    public ResponseEntity<Object> validateMetaData(@Validated @RequestBody MetaData metaData)
        throws JsonProcessingException {

        metaDataService.validate(metaData);
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/metadata/{type}/{id}")
    public boolean remove(@PathVariable("type") String type,
                          @PathVariable("id") String id,
                          @RequestBody(required = false) Map body,
                          FederatedUser user) {
        String defaultValue = "Deleted by " + user.getUid();
        String revisionNote = body != null ? (String) body.getOrDefault("revisionNote", defaultValue) : defaultValue;
        revisionNote = StringUtils.hasText(revisionNote) ? revisionNote : defaultValue;
        return metaDataService.doRemove(type, id, user, revisionNote);
    }

    @PreAuthorize("hasRole('DELETE_SP')")
    @DeleteMapping("/internal/metadata/{type}/{id}")
    public boolean removeInternal(@PathVariable("type") String type,
                                  @PathVariable("id") String id,
                                  APIUser apiUser) {
        ScopeEnforcer.enforceDeleteScope(apiUser, EntityType.fromType(type));
        return metaDataService.doRemove(type, id, apiUser, "Deleted by APIUser " + apiUser.getName());
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/metadata")
    @Transactional
    public MetaData put(@Validated @RequestBody MetaData metaData, FederatedUser user)
        throws JsonProcessingException {
        return metaDataService.doPut(metaData, user, false);
    }

    @PreAuthorize("hasAnyRole('WRITE_SP', 'WRITE_IDP', 'SYSTEM')")
    @PutMapping("/internal/metadata")
    @Transactional
    public MetaData putInternal(@Validated @RequestBody MetaData metaData, APIUser apiUser)
        throws JsonProcessingException {
        ScopeEnforcer.enforceWriteScope(apiUser, EntityType.fromType(metaData.getType()));
        return metaDataService.doPut(metaData, apiUser, !apiUser.getScopes().contains(TEST));
    }

    @PreAuthorize("hasAnyRole('SYSTEM')")
    @PutMapping("/internal/removeExtraneousKeys/{type}")
    @Transactional
    public ResponseEntity<List<String>> removeExtraneousKeys(@PathVariable("type") String type, @RequestBody List<String> extraneousKeys, APIUser apiUser) {
        LOG.info("RemoveExtraneousKeys called by {}", apiUser.getName());

        List<String> results = new ArrayList<>();
        List<MetaData> metaDataEntries = metaDataRepository.findAllByType(type);
        metaDataEntries.forEach(metaData -> {
            Map<String, Object> metaDataFields = metaData.metaDataFields();
            Set<String> keySet = metaDataFields.keySet();
            if (keySet.stream().anyMatch(key -> extraneousKeys.contains(key))) {
                keySet.removeIf(key -> extraneousKeys.contains(key));

                LOG.info(String.format("Saving %s metadata where extraneousKeys are removed", metaData.getData().get("entityid")));

                metaDataRepository.update(metaData);
                results.add((String) metaData.getData().get("entityid"));
            }
        });
        return ResponseEntity.ok(results);
    }

    @PreAuthorize("hasAnyRole('WRITE_SP', 'WRITE_IDP', 'SYSTEM')")
    @PutMapping("/internal/delete-metadata-key")
    @Transactional
    public List<String> deleteMetaDataKey(@Validated @RequestBody MetaDataKeyDelete metaDataKeyDelete,
                                          APIUser apiUser) throws IOException {
        ScopeEnforcer.enforceWriteScope(apiUser, EntityType.fromType(metaDataKeyDelete.getType()));
        return metaDataService.deleteMetaDataKey(metaDataKeyDelete, apiUser);
    }

    @PreAuthorize("hasAnyRole('WRITE_SP', 'WRITE_IDP', 'SYSTEM')")
    @PutMapping("internal/merge")
    @Transactional
    public MetaData update(@Validated @RequestBody MetaDataUpdate metaDataUpdate, APIUser apiUser)
        throws JsonProcessingException {
        ScopeEnforcer.enforceWriteScope(apiUser, EntityType.fromType(metaDataUpdate.getType()));
        return metaDataService
            .doMergeUpdate(metaDataUpdate, apiUser, "Internal API merge", true)
            .get();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/change-requests/{type}/{metaDataId}")
    public List<MetaDataChangeRequest> changeRequests(@PathVariable("type") String type,
                                                      @PathVariable("metaDataId") String metaDataId) {
        return metaDataRepository.changeRequests(metaDataId, type.concat(CHANGE_REQUEST_POSTFIX));
    }

    @PreAuthorize("hasAnyRole('CHANGE_REQUEST_SP', 'CHANGE_REQUEST_IDP')")
    @GetMapping("/internal/change-requests/{type}/{metaDataId}")
    public List<MetaDataChangeRequest> internalChangeRequests(@PathVariable("type") String type,
                                                              @PathVariable("metaDataId") String metaDataId,
                                                              APIUser apiUser) {
        ScopeEnforcer.enforceChangeRequestScope(apiUser, EntityType.fromType(type));
        return metaDataRepository.changeRequests(metaDataId, type.concat(CHANGE_REQUEST_POSTFIX));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/change-requests/all")
    public List<MetaDataChangeRequest> allChangeRequests() {
        return metaDataRepository.allChangeRequests();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("client/change-requests/count")
    public long openChangeRequests() {
        return metaDataRepository.openChangeRequests();
    }


    @PreAuthorize("hasAnyRole('CHANGE_REQUEST_SP', 'CHANGE_REQUEST_IDP')")
    @PostMapping("internal/change-requests")
    @Transactional
    public MetaDataChangeRequest changeRequestInternal(@Validated @RequestBody MetaDataChangeRequest metaDataChangeRequest, APIUser apiUser) throws JsonProcessingException {
        ScopeEnforcer.enforceChangeRequestScope(apiUser, EntityType.fromType(metaDataChangeRequest.getType()));
        return metaDataService.doChangeRequest(metaDataChangeRequest, apiUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("client/change-requests")
    @Transactional
    public MetaDataChangeRequest changeRequestClient(@Validated @RequestBody MetaDataChangeRequest metaDataChangeRequest, FederatedUser federatedUser) throws JsonProcessingException {
        return metaDataService.doChangeRequest(metaDataChangeRequest, federatedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/change-requests/accept")
    @Transactional
    public MetaData acceptChangeRequest(@RequestBody @Validated ChangeRequest changeRequest, FederatedUser user) throws JsonProcessingException {
        String collectionName = changeRequest.getType().concat(CHANGE_REQUEST_POSTFIX);
        MetaDataChangeRequest metaDataChangeRequest = metaDataRepository.getMongoTemplate()
            .findById(changeRequest.getId(), MetaDataChangeRequest.class, collectionName);

        MetaData metaData = metaDataService
            .doMergeUpdate(metaDataChangeRequest, user, changeRequest.getRevisionNotes(), true)
            .get();
        metaDataRepository.getMongoTemplate().remove(metaDataChangeRequest, collectionName);
        return metaData;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/change-requests/reject")
    @Transactional
    public MetaData rejectChangeRequest(@RequestBody @Validated ChangeRequest changeRequest, FederatedUser user) {
        return metaDataService.doRejectChangeRequest(changeRequest, user);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/client/change-requests/remove/{type}/{metaDataId}")
    @Transactional
    public DeleteResult removeChangeRequest(@PathVariable("type") String type,
                                            @PathVariable("metaDataId") String metaDataId, FederatedUser user) {
        return metaDataService.doDeleteChangeRequest(type, metaDataId, user);
    }

    @PreAuthorize("hasAnyRole('CHANGE_REQUEST_IDP', 'CHANGE_REQUEST_SP')")
    @PutMapping("/internal/change-requests/reject")
    @Transactional
    public MetaData internalRejectChangeRequest(@RequestBody @Validated ChangeRequest changeRequest, APIUser apiUser) {
        ScopeEnforcer.enforceChangeRequestScope(apiUser, EntityType.fromType(changeRequest.getType()));
        return metaDataService.doRejectChangeRequest(changeRequest, apiUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/restoreDeleted")
    @Transactional
    public MetaData restoreDeleted(@Validated @RequestBody RevisionRestore revisionRestore,
                                   FederatedUser federatedUser) throws JsonProcessingException {

        return metaDataService.restoreDeleted(revisionRestore, federatedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/restoreRevision")
    @Transactional
    public MetaData restoreRevision(@Validated @RequestBody RevisionRestore revisionRestore,
                                    FederatedUser federatedUser) throws JsonProcessingException {

        return metaDataService.restoreRevision(revisionRestore, federatedUser);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/revisions/{type}/{parentId}")
    public List<MetaData> revisions(@PathVariable("type") String type,
                                    @PathVariable("parentId") String parentId) {
        return metaDataRepository.revisions(type.concat(REVISION_POSTFIX), parentId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/autocomplete/{type}")
    public Map<String, List<Map>> autoCompleteEntities(@PathVariable("type") String type,
                                                       @RequestParam("query") String query) {

        return metaDataService.autoCompleteEntities(type, query);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/whiteListing/{type}")
    public List<Map> whiteListing(@PathVariable("type") String type, @RequestParam(value = "state") String state) {
        return metaDataRepository.whiteListing(type, state);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/relyingParties")
    public List<Map> relyingParties(@RequestParam("resourceServerEntityID") String resourceServerEntityID) {
        return metaDataRepository.relyingParties(resourceServerEntityID);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/spPolicies")
    public List<Map> spPolicies(@RequestParam("entityId") String entityId) {
        return metaDataRepository.spPolicies(entityId);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/idpPolicies")
    public List<Map> idpPolicies(@RequestParam("entityId") String entityId) {
        return metaDataRepository.idpPolicies(entityId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @PostMapping({"/client/provisioning", "/internal/provisioning"})
    public List<Map> provisioning(@RequestBody List<String> identifiers) {
        return metaDataRepository.provisioning(identifiers);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping({"/client/metadata/allentities"})
    public List<MetaData> getAllEntities() {
        return metaDataRepository.retrieveAllEntities();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @GetMapping({"/client/allowedEntities/{type}/{id}", "/internal/allowedEntities/{type}/{id}"})
    public List<Map> allowedEntities(@PathVariable("type") String type, @PathVariable("id") String id) {
        return metaDataRepository.allowedEntities(id, EntityType.fromType(type));
    }


    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @PostMapping({"/client/uniqueEntityId/{type}", "/internal/uniqueEntityId/{type}"})
    public List<Map> uniqueEntityId(@PathVariable("type") String type, @RequestBody Map<String, Object> properties) {
        String entityId = (String) properties.get("entityid");
        return metaDataService.uniqueEntityId(type, entityId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @PostMapping({"/client/uniquePolicyName/policy", "/internal/uniquePolicyName/policy"})
    public List<MetaData> uniquePolicyName(@RequestBody Map<String, Object> properties) {
        String name = (String) properties.get("name");
        String query = "{ \"data.name\": { \"$regex\": \"^" + name + "$\", \"$options\": \"i\" } }";
        return metaDataRepository.findRaw(EntityType.PDP.getType(), query);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @PostMapping({"/client/search/{type}", "/internal/search/{type}"})
    public List<Map> searchEntities(@PathVariable("type") String type,
                                    @RequestBody Map<String, Object> properties,
                                    @RequestParam(required = false, defaultValue = "false") boolean nested) {

        return metaDataService.searchEntityByType(type, properties, nested);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @GetMapping({"/client/rawSearch/{type}", "/internal/rawSearch/{type}"})
    public List<MetaData> rawSearch(@PathVariable("type") String type, @RequestParam("query") String query)
        throws UnsupportedEncodingException {
        return metaDataService.retrieveRawSearch(type, query);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @PostMapping({"/client/rawSearch/{type}", "/internal/rawSearch/{type}"})
    public List<MetaData> rawSearchPost(@PathVariable("type") String type, @RequestBody String query)
        throws UnsupportedEncodingException {
        return metaDataService.retrieveRawSearch(type, query);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/metadata/validate-unique-field/{type}/{name}/{value}")
    public ResponseEntity<Boolean> validateUniqueField(@PathVariable String type,
                                                       @PathVariable String name,
                                                       @PathVariable String value) {

        this.metaDataRepository.validateFieldUnique(type, name, value);
        return ResponseEntity.status(HttpStatus.OK).body(true);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @PostMapping({"/client/recent-activity", "/internal/recent-activity"})
    public List<MetaData> recentActivity(@RequestBody(required = false) Map<String, Object> properties) {
        return metaDataService.retrieveRecentActivity(properties);
    }

    @PreAuthorize("hasRole('CHANGE_REQUEST_IDP')")
    @PutMapping(value = "/internal/connectWithoutInteraction")
    public HttpEntity<HttpStatus> connectWithoutInteraction(@RequestBody Map<String, String> connectionData,
                                                            APIUser apiUser) throws JsonProcessingException {
        LOG.debug("connectWithoutInteraction, connectionData: " + connectionData);
        metaDataService.createConnectWithoutInteraction(connectionData, apiUser);
        return new HttpEntity<>(HttpStatus.OK);
    }

    @PreAuthorize("hasRole('READ')")
    @GetMapping("/internal/stats")
    public Map<String, Long> stats() throws IOException {
        MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
        return Stream.of(EntityType.SP, EntityType.IDP, EntityType.RP)
            .collect(Collectors.toMap(entityType -> entityType.getType(), entityType ->
                mongoTemplate.count(new Query(), entityType.getType())));
    }

}
