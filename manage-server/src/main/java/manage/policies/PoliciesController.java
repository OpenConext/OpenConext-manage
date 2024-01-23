package manage.policies;

import manage.control.MetaDataController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PoliciesController {

    private static final Logger LOG = LoggerFactory.getLogger(MetaDataController.class);

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/template/{type}")
    public List<PdpPolicyDefinition> policies() {

    }
}
