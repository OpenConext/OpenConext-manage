package mr.control;

import mr.migration.JanusMigration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class PlayGroundController {

    private JanusMigration janusMigration;

    @Autowired
    public PlayGroundController(JanusMigration janusMigration) {
        this.janusMigration = janusMigration;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/migrate")
    public List<Map<String, Long>> migrate() {
        return janusMigration.doMigrate();
    }

}
