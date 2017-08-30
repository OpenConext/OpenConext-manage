package mr.control;

import mr.conf.MetaDataAutoConfiguration;
import mr.format.Importer;
import mr.migration.EntityType;
import mr.model.Import;
import mr.model.XML;
import org.everit.json.schema.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class ImportController {

    private Importer importer;

    @Autowired
    public ImportController(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.importer = new Importer(metaDataAutoConfiguration);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/endpoint")
    public Map<String, Object> importUrl(@Validated @RequestBody Import importRequest) {
        try {
            Resource resource = new UrlResource(new URL(importRequest.getUrl()));
            Map<String, Object> result = this.importer.importXML( resource, Optional.ofNullable(importRequest.getEntityId()));
            result.put("metadataurl", importRequest.getUrl());
            return result;
        } catch (IOException | XMLStreamException e) {
            return Collections.singletonMap("errors", Collections.singletonList(e.toString()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/xml")
    public Map<String, Object> importXml(@Validated @RequestBody XML container) {
        try {
            return this.importer.importXML(new ByteArrayResource(container.getXml().getBytes()), Optional.ofNullable(container.getEntityId()));
        } catch (IOException | XMLStreamException e) {
            return Collections.singletonMap("errors", Collections.singletonList(e.toString()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/json/{type}")
    public Map<String, Object> importJson(@PathVariable("type") String type, @RequestBody Map<String, Object> json) throws IOException, XMLStreamException {
        EntityType entityType = getType(type, json);
        try {
            return this.importer.importJSON(entityType, json);
        } catch (ValidationException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("errors", e.getAllMessages());
            result.put("type", entityType.getType());
            return result;
        }
    }

    private EntityType getType(String type, Map<String, Object> json) {
        EntityType entityType = EntityType.IDP.getType().equals(type) ?
            EntityType.IDP : EntityType.SP.getType().equals(type) ? EntityType.SP : null;
        if (entityType == null) {
            return EntityType.fromType(String.class.cast(json.get("type")));
        }
        return entityType;
    }

}
