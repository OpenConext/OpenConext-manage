package mr.migration;

import mr.AbstractIntegrationTest;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"migrate_data_from_janus=true"})
//@Ignore
public class JanusMigrationTest extends AbstractIntegrationTest {

    @Test
    public void migrate() throws Exception {
    }

    @Override
    protected boolean insertSeedData() {
        return false;
    }


}