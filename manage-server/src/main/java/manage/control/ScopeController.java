package manage.control;

import manage.model.Scope;
import manage.repository.ScopeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.websocket.server.PathParam;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@RestController
public class ScopeController {

    private static final Logger LOG = LoggerFactory.getLogger(ScopeController.class);

    private ScopeRepository scopeRepository;
    private List<String> supportedLanguages;

    @Autowired
    public ScopeController(ScopeRepository scopeRepository, @Value("${product.supported_languages}") String supportedLanguages) {
        this.scopeRepository = scopeRepository;
        this.supportedLanguages = Stream.of(supportedLanguages.split(",")).map(String::trim).collect(toList());
    }

    @GetMapping({"/client/scopes_languages"})
    List<String> supportedLanguages() {
        return supportedLanguages;
    }

    @GetMapping({"/client/scopes", "/internal/scopes"})
    List<Scope> allScopes() {
        return scopeRepository.findAll();
    }

    @DeleteMapping({"/client/scopes/{id}"})
    boolean delete(@PathVariable("id") String id) {
        LOG.info("Deleting scope {}", id);
        scopeRepository.delete(id);
        return true;
    }

    @GetMapping(value = "/client/fetch/{value}", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<String> fetchValues(@PathParam("value") String value) {
        return scopeRepository.findAll().stream().map(Scope::getName).collect(Collectors.toList());
    }

    @GetMapping({"/client/scopes/{id}"})
    Scope get(@PathVariable("id") String id) {
        return scopeRepository.findOne(id);
    }

    @PutMapping({"/client/scopes"})
    Scope update(@RequestBody Scope scope) {
        LOG.info("Updating scope {}", scope);
        return scopeRepository.save(scope);
    }

    @PostMapping({"/client/scopes"})
    Scope save(@RequestBody Scope scope) {
        LOG.info("Saving scope {}", scope);
        return scopeRepository.save(scope);
    }
}
