package manage.oidc;

import com.fasterxml.jackson.core.JsonProcessingException;
import manage.AbstractIntegrationTest;
import manage.model.MetaData;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;


public class OIDCPushEndpointTest extends AbstractIntegrationTest {

    @Test
    public void testPush() throws JsonProcessingException {
        List<MetaData> oidcClients = metaDataRepository.getMongoTemplate().findAll(MetaData.class, "oidc10_rp");

        assertEquals(2, oidcClients.size());
    }
}
