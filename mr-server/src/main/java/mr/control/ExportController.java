package mr.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import mr.conf.MetaDataAutoConfiguration;
import mr.exception.ResourceNotFoundException;
import mr.format.Exporter;
import mr.model.MetaData;
import mr.repository.MetaDataRepository;
import mr.shibboleth.FederatedUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static mr.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@RestController
public class ExportController {

    private List<String> excludeMetaDataOnlyKeys = Arrays.asList("allowedEntities", "arp", "disableConsent",
        "active", "manipulation");

    private Exporter exporter;

    private ObjectMapper objectMapper;

    @Autowired
    public ExportController(ObjectMapper objectMapper, ResourceLoader resourceLoader,
                            @Value("${metadata_export_path}") String metadataExportPath) {
        this.objectMapper = objectMapper;
        this.exporter = new Exporter(Clock.systemDefaultZone(), resourceLoader, metadataExportPath);
    }

    @PostMapping(value = "/client/export")
    public Map<String, Object> export(@RequestBody MetaData metaData) throws IOException {
        Map<String, Object> result = new HashMap<>();

        metaData.getData().entrySet().removeIf(entry-> entry.getValue() == null);

        Map<String, Object> nested = exporter.exportToMap(metaData, true);
        Map<String, Object> flat = exporter.exportToMap(metaData, false);

        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();

        result.put("json", objectWriter.writeValueAsString(nested));
        result.put("jsonFlat", objectWriter.writeValueAsString(flat));
        result.put("xml", exporter.exportToXml(metaData));

        Map<String, Object> metaDataOnlyNested = new TreeMap<>(nested);
        excludeMetaDataOnlyKeys.forEach(metaDataOnlyNested::remove);
        result.put("jsonMetaDataOnly", objectWriter.writeValueAsString(metaDataOnlyNested));

        Map<String, Object> metaDataOnlyFlat = new TreeMap<>(flat);
        excludeMetaDataOnlyKeys.forEach(metaDataOnlyFlat::remove);
        result.put("jsonMetaDataOnlyFlat", objectWriter.writeValueAsString(metaDataOnlyFlat));

        return result;
    }

}
