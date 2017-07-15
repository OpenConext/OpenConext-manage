package mr.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import mr.conf.MetaDataAutoConfiguration;
import mr.format.Exporter;
import mr.format.Importer;
import mr.migration.EntityType;
import mr.model.MetaData;
import mr.model.XML;
import org.everit.json.schema.ValidationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.time.Clock;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ImportController {

    private Importer importer;

    @Autowired
    public ImportController(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.importer = new Importer(metaDataAutoConfiguration);
    }

    @GetMapping(value = "/client/import/endpoint/{type}/{url}")
    public Map<String, Object> importUrl(@PathVariable("type") String type,
                                         @PathVariable("url") String url) throws IOException, XMLStreamException {
        return this.importer.importURL(EntityType.IDP.getType().equals(type) ? EntityType.IDP : EntityType.SP, url);
    }

    @PostMapping(value = "/client/import/xml/{type}")
    public Map<String, Object> importXml(@PathVariable("type") String type, @Validated @RequestBody XML container) throws IOException, XMLStreamException {
        String xml = container.getXml();
        if (xml.startsWith("\"")) {
            xml = xml.substring(1);
        }
        if (xml.endsWith("\"")) {
            xml = xml.substring(0, xml.length() - 1);
        }
        return this.importer.importXML(EntityType.IDP.getType().equals(type) ? EntityType.IDP : EntityType.SP, xml);
    }

    @PostMapping(value = "/client/import/json/{type}")
    public Map<String, Object> importJson(@PathVariable("type") String type, @RequestBody Map<String, Object> json) throws IOException, XMLStreamException {
        try {
            return this.importer.importJSON(EntityType.IDP.getType().equals(type) ? EntityType.IDP : EntityType.SP, json);
        } catch (ValidationException e) {
             return Collections.singletonMap("errors", e.toJSON().toMap());
        }
    }

}
