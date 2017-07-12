package mr.control;

import mr.migration.JanusMigration;
import mr.migration.JanusMigrationValidation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class PlayGroundController {

    private JanusMigrationValidation janusMigrationValidation;
    private JanusMigration janusMigration;

    @Autowired
    public PlayGroundController(JanusMigration janusMigration, JanusMigrationValidation janusMigrationValidation) {
        this.janusMigration = janusMigration;
        this.janusMigrationValidation = janusMigrationValidation;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/migrate")
    public List<Map<String, Long>> migrate() {
        return janusMigration.doMigrate();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/playground/validate")
    public Map<String, Object> validate() {
        return janusMigrationValidation.validateMigration();
    }
}
