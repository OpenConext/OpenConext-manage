package manage.control;

import manage.model.EntityType;
import manage.model.Import;
import manage.model.XML;
import manage.service.ImporterService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@RestController
public class ImportController {

    private ImporterService importerService;

    public ImportController(ImporterService importerService) {
        this.importerService = importerService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/endpoint/xml/{type}")
    public Map<String, Object> importXMLUrl(@PathVariable("type") String type,
                                            @Validated @RequestBody Import importRequest) {

        return importerService.importXMLUrl(EntityType.fromType(type), importRequest);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/xml/{type}")
    public Map<String, Object> importXml(@PathVariable("type") String type, @Validated @RequestBody XML container) {
        try {
            return this.importerService.importXML(new ByteArrayResource(container.getXml().getBytes()),
                EntityType.fromType(type), Optional.ofNullable(container.getEntityId()));
        } catch (IOException | XMLStreamException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/json/{type}")
    public Map<String, Object> importJson(@PathVariable("type") String type, @RequestBody Map<String, Object> json)
            throws IOException {

        return importerService.importJson(type, json);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/endpoint/json/{type}")
    public Map<String, Object> importJsonUrl(@PathVariable("type") String type,
                                             @Validated @RequestBody Import importRequest) {

        return importerService.importJsonUrl(type, importRequest);
    }

}
