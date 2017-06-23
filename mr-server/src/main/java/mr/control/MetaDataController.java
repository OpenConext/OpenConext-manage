package mr.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mr.conf.MetadataAutoConfiguration;
import mr.model.MetaData;
import mr.mongo.MongobeeConfiguration;
import mr.repository.MetaDataRepository;
import mr.shibboleth.FederatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class MetaDataController {

    @Autowired
    private MetaDataRepository metaDataRepository;

    @Autowired
    private MetadataAutoConfiguration metadataAutoConfiguration;

    @Autowired
    private ObjectMapper objectMapper;

    @GetMapping("/client/metadata/{type}/{id}")
    public MetaData get(@PathVariable("type") String type, @PathVariable("id") String id) {
        return metaDataRepository.findById(id, type);
    }

    @PostMapping("/client/metadata")
    public MetaData post(@Validated @RequestBody MetaData metaData, FederatedUser federatedUser) throws JsonProcessingException {
        validate(metaData);

        metaData.initial(UUID.randomUUID().toString(), federatedUser.uid);
        return metaDataRepository.save(metaData);
    }

    @PutMapping("/client/metadata")
    @Transactional
    public MetaData put(@Validated @RequestBody MetaData metaData, FederatedUser federatedUser) throws JsonProcessingException {
        validate(metaData);

        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());
        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest(federatedUser.uid);
        metaDataRepository.update(metaData);

        return metaData;
    }

    @GetMapping("/client/revisions/{type}/{parentId}")
    public List<MetaData> revisions(@PathVariable("type") String type, @PathVariable("parentId") String parentId) {
        return metaDataRepository.revisions(type.concat(MongobeeConfiguration.REVISION_POSTFIX), parentId);
    }

    @GetMapping("/client/autocomplete/{type}")
    public List<Map> autoCompleteEntities(@PathVariable("type") String type, @RequestParam("query") String query) {
        return metaDataRepository.autoComplete(type, query);
    }

    private void validate(MetaData metaData) throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(metaData.getData());
        metadataAutoConfiguration.validate(json, metaData.getType());
    }

}
