package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.api.APIUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.exception.DuplicateEntityIdException;
import manage.exception.ResourceNotFoundException;
import manage.format.Exporter;
import manage.format.Importer;
import manage.migration.EntityType;
import manage.model.MetaData;
import manage.model.MetaDataUpdate;
import manage.model.XML;
import manage.repository.MetaDataRepository;
import manage.shibboleth.FederatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
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
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static manage.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@RestController
public class MetaDataController {

    public static final String REQUESTED_ATTRIBUTES = "REQUESTED_ATTRIBUTES";

    private MetaDataRepository metaDataRepository;
    private MetaDataAutoConfiguration metaDataAutoConfiguration;
    private Importer importer;
    private Exporter exporter;

    @Autowired
    public MetaDataController(MetaDataRepository metaDataRepository,
                              MetaDataAutoConfiguration metaDataAutoConfiguration,
                              ResourceLoader resourceLoader,
                              @Value("${metadata_export_path}") String metadataExportPath) {
        this.metaDataRepository = metaDataRepository;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.importer = new Importer(metaDataAutoConfiguration);
        this.exporter = new Exporter(Clock.systemDefaultZone(), resourceLoader, metadataExportPath);

    }

    @GetMapping("/client/template/{type}")
    public MetaData template(@PathVariable("type") String type) {
        return new MetaData(type, Map.class.cast(metaDataAutoConfiguration.metaDataTemplate(type)));
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
            .getBytes()), Optional.empty());
        String entityId = String.class.cast(innerJson.get("entityid"));
        List<Map> result = metaDataRepository.search(EntityType.SP.getType(), singletonMap("entityid",
            entityId), emptyList());

        if (!CollectionUtils.isEmpty(result)) {
            throw new DuplicateEntityIdException(entityId);
        }

        addDefaultSpData(innerJson);
        MetaData metaData = new MetaData(EntityType.SP.getType(), innerJson);

        return doPost(metaData, apiUser.getName());
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
            .getBytes()), Optional.empty());

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

        metaData.initial(UUID.randomUUID().toString(), uid);
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
    public boolean remove(@PathVariable("type") String type, @PathVariable("id") String id) throws
        JsonProcessingException {
        MetaData current = metaDataRepository.findById(id, type);
        metaDataRepository.remove(current);
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
        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest(updatedBy);
        metaDataRepository.update(metaData);

        return metaData;
    }

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("internal/merge")
    @Transactional
    public MetaData update(@Validated @RequestBody MetaDataUpdate metaDataUpdate, APIUser apiUser) throws
        JsonProcessingException {
        String id = metaDataUpdate.getId();
        MetaData previous = metaDataRepository.findById(id, metaDataUpdate.getType());
        previous.revision(UUID.randomUUID().toString());

        MetaData metaData = metaDataRepository.findById(id, metaDataUpdate.getType());
        metaData.promoteToLatest(apiUser.getName());
        metaData.merge(metaDataUpdate);

        validate(metaData);

        metaDataRepository.save(previous);
        metaDataRepository.update(metaData);

        return metaData;
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
    public List<Map> searchEntities(@PathVariable("type") String type, @RequestBody Map<String, Object> properties) {
        List requestedAttributes = List.class.cast(properties.getOrDefault(REQUESTED_ATTRIBUTES, new
            ArrayList<String>()));
        properties.remove(REQUESTED_ATTRIBUTES);
        return metaDataRepository.search(type, properties, requestedAttributes);
    }

    private void validate(MetaData metaData) throws JsonProcessingException {
        String json = metaDataAutoConfiguration.getObjectMapper().writeValueAsString(metaData.getData());
        metaDataAutoConfiguration.validate(json, metaData.getType());
    }

}
