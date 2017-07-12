package mr.migration;

import mr.AbstractIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Ignore
public class JanusMigrationValidationTest extends AbstractIntegrationTest {

    @Autowired
    private JanusMigrationValidation janusMigrationValidation;

    @Test
    public void validateMigrate() throws Exception {
        janusMigrationValidation.validateMigration();
    }

    @Override
    protected boolean insertSeedData() {
        return false;
    }


}