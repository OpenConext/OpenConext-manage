package manage.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import manage.conf.MetaDataAutoConfiguration;
import manage.format.Importer;
import manage.format.SaveURLResource;
import manage.model.EntityType;
import manage.model.Import;
import manage.model.XML;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@RestController
public class ImportController {

    private Importer importer;
    private ObjectMapper objectMapper;
    private Environment environment;

    @Autowired
    public ImportController(MetaDataAutoConfiguration metaDataAutoConfiguration, ObjectMapper objectMapper,
                            Environment environment) {
        this.importer = new Importer(metaDataAutoConfiguration);
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/endpoint/xml/{type}")
    public Map<String, Object> importXMLUrl(@PathVariable("type") String type, @Validated @RequestBody Import
        importRequest) {
        try {
            Resource resource = new SaveURLResource(new URL(importRequest.getUrl()), environment.acceptsProfiles("dev"));
            Map<String, Object> result = this.importer.importXML(resource, EntityType.fromType(type), Optional
                .ofNullable(importRequest
                .getEntityId()));
            result.put("metadataurl", importRequest.getUrl());
            return result;
        } catch (IOException | XMLStreamException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/xml/{type}")
    public Map<String, Object> importXml(@PathVariable("type") String type, @Validated @RequestBody XML container) {
        try {
            return this.importer.importXML(new ByteArrayResource(container.getXml().getBytes()), EntityType.fromType
                (type), Optional.ofNullable
                (container.getEntityId()));
        } catch (IOException | XMLStreamException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/json/{type}")
    public Map<String, Object> importJson(@PathVariable("type") String type, @RequestBody Map<String, Object> json)
        throws IOException {
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

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/import/endpoint/json/{type}")
    public Map<String, Object> importJsonUrl(@PathVariable("type") String type, @Validated @RequestBody Import
        importRequest) {
        try {
            Resource resource = new SaveURLResource(new URL(importRequest.getUrl()),environment.acceptsProfiles("dev"));
            String json = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());
            Map map = objectMapper.readValue(json, Map.class);
            return this.importJson(type, map);
        } catch (IOException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
        }
    }

    private EntityType getType(String type, Map<String, Object> json) {
        EntityType entityType = EntityType.IDP.getType().equals(type) ?
            EntityType.IDP : EntityType.SP.getType().equals(type) ? EntityType.SP : null;
        if (entityType == null) {
            Object jsonType = json.get("type");
            if (jsonType == null) {
                throw new IllegalArgumentException("Expected a 'type' attribute in the JSON with value 'saml20-idp' " +
                    "or 'saml20-sp'");
            }
            return EntityType.fromType(String.class.cast(jsonType));
        }
        return entityType;
    }

}
