package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.api.APIUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.exception.ResourceNotFoundException;
import manage.model.MetaData;
import manage.model.MetaDataUpdate;
import manage.repository.MetaDataRepository;
import manage.shibboleth.FederatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static manage.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@RestController
public class MetaDataController {

    public static final String REQUESTED_ATTRIBUTES = "REQUESTED_ATTRIBUTES";

    @Autowired
    private MetaDataRepository metaDataRepository;

    @Autowired
    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    @GetMapping("/client/template/{type}")
    public MetaData template(@PathVariable("type") String type) {
        return new MetaData(type, Map.class.cast(metaDataAutoConfiguration.metaDataTemplate(type)));
    }

    @GetMapping("/client/metadata/{type}/{id}")
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
        validate(metaData);

        metaData.initial(UUID.randomUUID().toString(), federatedUser.getUid());
        return metaDataRepository.save(metaData);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/client/metadata/{type}/{id}")
    public boolean remove(@PathVariable("type") String type, @PathVariable("id") String id, FederatedUser
        federatedUser) throws JsonProcessingException {
        MetaData current = metaDataRepository.findById(id, type);
        metaDataRepository.remove(current);
        return true;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/client/metadata")
    @Transactional
    public MetaData put(@Validated @RequestBody MetaData metaData, FederatedUser federatedUser) throws
        JsonProcessingException {
        validate(metaData);

        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());
        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest(federatedUser.getUid());
        metaDataRepository.update(metaData);

        return metaData;
    }

    @PreAuthorize("hasRole('WRITE')")
    @PutMapping("internal/metadata")
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
