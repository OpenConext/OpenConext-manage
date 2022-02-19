package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.api.APIUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.*;
import manage.repository.MetaDataRepository;
import manage.service.ExporterService;
import manage.service.ImporterService;
import manage.service.MetaDataService;
import manage.shibboleth.FederatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static manage.api.Scope.TEST;
import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;
import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@RestController
@SuppressWarnings("unchecked")
public class MetaDataController {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataController.class);

    private MetaDataRepository metaDataRepository;

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    private MetaDataService metaDataService;

    private ExporterService exporterService;

    private ImporterService importerService;

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

    @GetMapping("/client/template/{type}")
    public MetaData template(@PathVariable("type") String type) {
        Map<String, Object> data = metaDataAutoConfiguration.metaDataTemplate(type);
        return new MetaData(type, data);
    }

    @GetMapping({"/client/metadata/{type}/{id}", "/internal/metadata/{type}/{id}"})
    public MetaData get(@PathVariable("type") String type, @PathVariable("id") String id) {
        return metaDataService.getMetaDataAndValidate(type, id);
    }

    @GetMapping("/client/metadata/configuration")
    public List<Map<String, Object>> configuration() {
        return metaDataAutoConfiguration.schemaRepresentations();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/client/metadata")
    public MetaData post(@Validated @RequestBody MetaData metaData, FederatedUser federatedUser)
            throws JsonProcessingException {

        return metaDataService.doPost(metaData, federatedUser.getUid(), false);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/includeInPush/{type}/{id}")
    public MetaData includeInPush(@PathVariable("type") String type,
                                  @PathVariable("id") String id,
                                  FederatedUser federatedUser) throws JsonProcessingException {

        MetaData metaData = this.get(type, id);
        Map metaDataFields = metaData.metaDataFields();
        metaDataFields.remove("coin:exclude_from_push");

        return metaDataService.doPut(metaData, federatedUser.getUid(), false);
    }

    @GetMapping("/client/metadata/stats")
    public List<StatsEntry> stats() {
        return metaDataRepository.stats();
    }

    @PreAuthorize("hasRole('WRITE')")
    @PostMapping("/internal/metadata")
    public MetaData postInternal(@Validated @RequestBody MetaData metaData, APIUser apiUser)
            throws JsonProcessingException {

        return metaDataService.doPost(metaData, apiUser.getName(), !apiUser.getScopes().contains(TEST));
    }

    @PreAuthorize("hasRole('WRITE')")
    @PostMapping("/internal/new-sp")
    public MetaData newSP(@Validated @RequestBody XML container, APIUser apiUser)
            throws IOException, XMLStreamException {

        Map<String, Object> innerJson = importerService.importXML(new ByteArrayResource(container.getXml()
                .getBytes()), EntityType.SP, Optional.empty());

        metaDataService.addDefaultSpData(innerJson);
        MetaData metaData = new MetaData(EntityType.SP.getType(), innerJson);

        return metaDataService.doPost(metaData, apiUser.getName(), !apiUser.getScopes().contains(TEST));
    }


    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(value = "/client/delete/feed")
    public Map<String, Long> deleteFeed() {
        long deleted = this.metaDataRepository.deleteAllImportedServiceProviders();
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
        return metaDataService.importFeed(importRequest);
    }

    @PreAuthorize("hasRole('WRITE')")
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

        return metaDataService.doPut(metaData, apiUser.getName(), !apiUser.getScopes().contains(TEST));
    }

    @GetMapping("/internal/sp-metadata/{id}")
    public String exportXml(@PathVariable("id") String id) throws IOException {
        MetaData metaData = metaDataService.getMetaDataAndValidate(EntityType.SP.getType(), id);
        return exporterService.exportToXml(metaData);
    }

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
        return metaDataService.doRemove(type, id, user.getUid(), revisionNote);
    }

    @PreAuthorize("hasRole('WRITE')")
    @DeleteMapping("/internal/metadata/{type}/{id}")
    public boolean removeInternal(@PathVariable("type") String type,
                                  @PathVariable("id") String id, APIUser apiUser) {

        return metaDataService.doRemove(type, id, apiUser.getName(), "Deleted by APIUser " + apiUser.getName());
    }


    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/metadata")
    @Transactional
    public MetaData put(@Validated @RequestBody MetaData metaData, Authentication authentication)
            throws JsonProcessingException {

        return metaDataService.doPut(metaData, authentication.getName(), false);
    }

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("/internal/metadata")
    @Transactional
    public MetaData putInternal(@Validated @RequestBody MetaData metaData, APIUser apiUser)
            throws JsonProcessingException {

        return metaDataService.doPut(metaData, apiUser.getName(), !apiUser.getScopes().contains(TEST));
    }

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("/internal/delete-metadata-key")
    @Transactional
    public List<String> deleteMetaDataKey(@Validated @RequestBody MetaDataKeyDelete metaDataKeyDelete,
                                          APIUser apiUser) throws IOException {

        return metaDataService.deleteMetaDataKey(metaDataKeyDelete, apiUser);
    }

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("internal/merge")
    @Transactional
    public MetaData update(@Validated @RequestBody MetaDataUpdate metaDataUpdate, APIUser apiUser)
            throws JsonProcessingException {
        String name = apiUser.getName();
        return metaDataService
                .doMergeUpdate(metaDataUpdate, name, "Internal API merge", true)
                .get();
    }

    @GetMapping("/client/change-requests/{type}/{metaDataId}")
    public List<MetaDataChangeRequest> changeRequests(@PathVariable("type") String type,
                                                      @PathVariable("metaDataId") String metaDataId) {
        return metaDataRepository.changeRequests(metaDataId, type.concat(CHANGE_REQUEST_POSTFIX));
    }

    @PreAuthorize("hasRole('WRITE')")
    @GetMapping("/internal/change-requests/{type}/{metaDataId}")
    public List<MetaDataChangeRequest> internalChangeRequests(@PathVariable("type") String type,
                                                              @PathVariable("metaDataId") String metaDataId) {
        return metaDataRepository.changeRequests(metaDataId, type.concat(CHANGE_REQUEST_POSTFIX));
    }

    @GetMapping("/client/change-requests/all")
    public List<MetaDataChangeRequest> allChangeRequests() {
        return metaDataRepository.allChangeRequests();
    }

    @GetMapping("client/change-requests/count")
    public long openChangeRequests() {
        return metaDataRepository.openChangeRequests();
    }


    @PreAuthorize("hasRole('WRITE')")
    @PostMapping("internal/change-requests")
    @Transactional
    public MetaDataChangeRequest changeRequestInternal(@Validated @RequestBody MetaDataChangeRequest metaDataChangeRequest, APIUser apiUser) throws JsonProcessingException {
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
        String name = user.getName();
        String collectionName = changeRequest.getType().concat(CHANGE_REQUEST_POSTFIX);
        MetaDataChangeRequest metaDataChangeRequest = metaDataRepository.getMongoTemplate().findById(changeRequest.getId(), MetaDataChangeRequest.class, collectionName);

        MetaData metaData = metaDataService
                .doMergeUpdate(metaDataChangeRequest, name, "Change request API merge", true)
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

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("/internal/change-requests/reject")
    @Transactional
    public MetaData internalRejectChangeRequest(@RequestBody @Validated ChangeRequest changeRequest, APIUser user) {
        return metaDataService.doRejectChangeRequest(changeRequest, user);
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

    @GetMapping("/client/revisions/{type}/{parentId}")
    public List<MetaData> revisions(@PathVariable("type") String type,
                                    @PathVariable("parentId") String parentId) {

        return metaDataRepository.revisions(type.concat(REVISION_POSTFIX), parentId);
    }

    @GetMapping("/client/autocomplete/{type}")
    public Map<String, List<Map>> autoCompleteEntities(@PathVariable("type") String type,
                                                       @RequestParam("query") String query) {

        return metaDataService.autoCompleteEntities(type, query);
    }

    @GetMapping("/client/whiteListing/{type}")
    public List<Map> whiteListing(@PathVariable("type") String type, @RequestParam(value = "state") String state) {
        return metaDataRepository.whiteListing(type, state);
    }

    @PostMapping({"/client/uniqueEntityId/{type}", "/internal/uniqueEntityId/{type}"})
    public List<Map> uniqueEntityId(@PathVariable("type") String type, @RequestBody Map<String, Object> properties) {
        String entityId = (String) properties.get("entityid");
        return metaDataService.uniqueEntityId(type, entityId);
    }

    @PostMapping({"/client/search/{type}", "/internal/search/{type}"})
    public List<Map> searchEntities(@PathVariable("type") String type,
                                    @RequestBody Map<String, Object> properties,
                                    @RequestParam(required = false, defaultValue = "false") boolean nested) {

        return metaDataService.searchEntityByType(type, properties, nested);
    }

    @GetMapping({"/client/rawSearch/{type}", "/internal/rawSearch/{type}"})
    public List<MetaData> rawSearch(@PathVariable("type") String type, @RequestParam("query") String query)
            throws UnsupportedEncodingException {

        return metaDataService.retrieveRawSearch(type, query);
    }

    @PostMapping({"/client/recent-activity", "/internal/recent-activity"})
    public List<MetaData> recentActivity(@RequestBody(required = false) Map<String, Object> properties) {
        return metaDataService.retrieveRecentActivity(properties);
    }

    @Secured("WRITE")
    @PutMapping(value = "/internal/connectWithoutInteraction")
    public HttpEntity<HttpStatus> connectWithoutInteraction(@RequestBody Map<String, String> connectionData,
                                                            APIUser apiUser) throws JsonProcessingException {

        LOG.debug("connectWithoutInteraction, connectionData: " + connectionData);
        metaDataService.createConnectWithoutInteraction(connectionData, apiUser);
        return new HttpEntity<>(HttpStatus.OK);
    }

}
