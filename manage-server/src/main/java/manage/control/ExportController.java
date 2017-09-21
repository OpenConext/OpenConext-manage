package manage.control;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import manage.format.Exporter;
import manage.model.MetaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
