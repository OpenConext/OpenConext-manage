package manage.control;

import manage.service.jobs.MetadataAutoRefreshRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/client/startreaper")
@PreAuthorize("hasRole('ADMIN')")
public class ReaperController {

    private static final Logger LOG = LoggerFactory.getLogger(ReaperController.class);

    private final MetadataAutoRefreshRunner metadataAutoRefreshRunner;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public ReaperController(MetadataAutoRefreshRunner metadataAutoRefreshRunner,
                            ThreadPoolTaskExecutor threadPoolTaskExecutor) {

        this.metadataAutoRefreshRunner = metadataAutoRefreshRunner;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
    }

    @GetMapping("metadataRefresh")
    public ResponseEntity<Boolean> startMetadataRefresh() {
        LOG.info("Started metadata refresh reaper manually");
        threadPoolTaskExecutor.execute(metadataAutoRefreshRunner);
        return ResponseEntity.status(HttpStatus.OK).body(true);
    }

}
