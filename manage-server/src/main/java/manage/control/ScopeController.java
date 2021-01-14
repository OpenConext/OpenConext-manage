package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.DuplicateKeyException;
import manage.exception.ResourceNotFoundException;
import manage.exception.ScopeDuplicateNameException;
import manage.exception.ScopeInUseException;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.Scope;
import manage.repository.ScopeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.server.PathParam;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@RestController
public class ScopeController {

    private static final Logger LOG = LoggerFactory.getLogger(ScopeController.class);
    private ObjectMapper objectMapper;

    private MongoTemplate mongoTemplate;
    private ScopeRepository scopeRepository;
    private List<String> supportedLanguages;

    @Autowired
    public ScopeController(MongoTemplate mongoTemplate,
                           ScopeRepository scopeRepository,
                           ObjectMapper objectMapper,
                           @Value("${product.supported_languages}") String supportedLanguages) {
        this.mongoTemplate = mongoTemplate;
        this.scopeRepository = scopeRepository;
        this.objectMapper = objectMapper;
        this.supportedLanguages = Stream.of(supportedLanguages.split(",")).map(String::trim).collect(toList());
    }

    @GetMapping({"/client/scopes_languages"})
    public List<String> supportedLanguages() {
        return supportedLanguages;
    }

    @GetMapping({"/client/scopes", "/internal/scopes"})
    public List<Scope> allScopes() {
        return scopeRepository.findAll();
    }

    @DeleteMapping({"/client/scopes/{id}"})
    public boolean delete(@PathVariable("id") String id) throws JsonProcessingException {
        Scope scope = scopeById(id);
        checkScopeInUse(scope);
        LOG.info("Deleting scope {}", id);
        scopeRepository.delete(scope);
        return true;
    }

    private Scope scopeById(String id) {
        Scope scope = scopeRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException(String.format("Scope with %s not found", id)));
        return scope;
    }

    @GetMapping(value = "/client/fetch/{value}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> fetchValues(@PathParam("value") String value) {
        return scopeRepository.findAll().stream().map(Scope::getName).collect(Collectors.toList());
    }

    @GetMapping({"/client/scopes/{id}"})
    public Scope get(@PathVariable("id") String id) {
        return scopeById(id);
    }

    @PutMapping({"/client/scopes"})
    public Scope update(@RequestBody Scope scope) throws JsonProcessingException {
        Scope previous = scopeById(scope.getId());
        if (!previous.getName().equals(scope.getName())) {
            checkScopeInUse(previous);
        }

        LOG.info("Updating scope {}", scope);
        return saveScope(scope);
    }

    private Scope saveScope(@RequestBody Scope scope) {
        try {
            return scopeRepository.save(scope);
        } catch (DuplicateKeyException | org.springframework.dao.DuplicateKeyException e) {
            throw new ScopeDuplicateNameException(e.getMessage());
        }
    }

    @PostMapping({"/client/scopes"})
    public Scope save(@RequestBody Scope scope) {
        LOG.info("Saving scope {}", scope);
        return saveScope(scope);
    }

    private void checkScopeInUse(Scope scope) throws JsonProcessingException {
        Query query = Query.query(Criteria.where("data.metaDataFields.scopes").is(scope.getName()));
        List<MetaData> resourcesServers = mongoTemplate.find(query, MetaData.class, EntityType.RS.getType());
        List<MetaData> relyingParties = mongoTemplate.find(query, MetaData.class, EntityType.RP.getType());
        resourcesServers.addAll(relyingParties);
        if (!resourcesServers.isEmpty()) {
            List<Map<String, String>> message = new ArrayList<>();
            resourcesServers.forEach(md -> {
                Map<String, String> entry = new HashMap<>();
                entry.put("id", md.getId());
                entry.put("type", md.getType());
                entry.put("entityid", (String) md.getData().get("entityid"));
                message.add(entry);
            });
            throw new ScopeInUseException(objectMapper.writeValueAsString(message));
        }
    }
}
