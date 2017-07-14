package mr.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import mr.conf.MetaDataAutoConfiguration;
import mr.exception.ResourceNotFoundException;
import mr.format.Exporter;
import mr.model.MetaData;
import mr.repository.MetaDataRepository;
import mr.shibboleth.FederatedUser;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static mr.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@RestController
public class ExportController {

    private Exporter exporter = new Exporter(Clock.systemDefaultZone());

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping(value = "/client/export")
    public Map<String, Object> export(@RequestBody MetaData metaData) throws IOException {
        Map<String, Object> result = new HashMap<>();

        result.put("json", objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(exporter.exportToMap(metaData, false)));
        result.put("jsonFlat", objectMapper.writerWithDefaultPrettyPrinter()
            .writeValueAsString(exporter.exportToMap(metaData, true)));
        result.put("xml", exporter.exportToXml(metaData));

        return result;
    }
}
