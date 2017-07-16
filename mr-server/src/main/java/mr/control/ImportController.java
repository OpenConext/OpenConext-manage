package mr.control;

import mr.conf.MetaDataAutoConfiguration;
import mr.format.Importer;
import mr.migration.EntityType;
import mr.model.Import;
import mr.model.XML;
import mr.validations.XMLFormatValidator;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@RestController
public class ImportController {

    private Importer importer;

    private XMLFormatValidator xmlFormatValidator = new XMLFormatValidator();

    @Autowired
    public ImportController(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.importer = new Importer(metaDataAutoConfiguration);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/endpoint")
    public Map<String, Object> importUrl(@Validated @RequestBody Import importRequest) throws IOException, XMLStreamException {
        URL url = new URL(importRequest.getUrl());
        String xml = IOUtils.toString(url.openConnection().getInputStream(), Charset.defaultCharset());

        Optional<String> validXml = xmlFormatValidator.validate(xml);
        if (validXml.isPresent()) {
            return Collections.singletonMap("errors", Collections.singletonList(validXml.get()));
        }

        Map<String, Object> result = this.importer.importXML(getType(importRequest.getType()), xml);
        result.put("metadataurl", importRequest.getUrl());
        return result;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/xml/{type}")
    public Map<String, Object> importXml(@PathVariable("type") String type, @Validated @RequestBody XML container) throws IOException, XMLStreamException {
        return this.importer.importXML(getType(type), container.getXml());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/json/{type}")
    public Map<String, Object> importJson(@PathVariable("type") String type, @RequestBody Map<String, Object> json) throws IOException, XMLStreamException {
        try {
            return this.importer.importJSON(getType(type), json);
        } catch (ValidationException e) {
            return Collections.singletonMap("errors", e.getAllMessages());
        }
    }

    private EntityType getType(String type) {
        EntityType entityType = EntityType.IDP.getType().equals(type) ? EntityType.IDP : EntityType.SP.getType().equals(type) ? EntityType.SP : null;
        if (entityType == null) {
            throw new IllegalArgumentException("Unknown entity type: " + type);
        }
        return entityType;
    }

    private void removeNulls(Map<String, Object> data) {
        data.entrySet().removeIf(entry-> entry.getValue() == null);;

    }
}
