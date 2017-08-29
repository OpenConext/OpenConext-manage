package mr.mongo;

import com.github.fakemongo.Fongo;
import mr.AbstractIntegrationTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.Set;

import static java.util.Arrays.asList;

public class MongobeeConfigurationTest extends AbstractIntegrationTest {

    @Autowired
    private MongobeeConfiguration subject;

    @Test
    public void testCreateCollections() {
        Fongo fongo = new Fongo("test");

        MongoTemplate mongoTemplate = new MongoTemplate(fongo.getMongo(), "test");
        subject.createCollections(mongoTemplate);

        Set<String> collectionNames = fongo.getDB("test").getCollectionNames();
        collectionNames.containsAll(asList("saml20_idp", "saml20_idp_revision", "saml20_sp", "saml20_sp_revision"));
    }

}