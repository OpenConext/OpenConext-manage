package manage.control;

import manage.model.MetaData;
import manage.service.ExporterService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
public class ExportController {

    private ExporterService exporterService;

    public ExportController(ExporterService exporterService) {
        this.exporterService = exporterService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/export")
    public Map<String, Object> export(@RequestBody MetaData metaData) throws IOException {
        return exporterService.export(metaData);
    }

}
