package manage.hook;

import manage.AbstractIntegrationTest;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class SSIDValidationHookTest extends AbstractIntegrationTest {

    private SSIDValidationHook subject;

    //http://mock-sp is a stepupEntities in IdP https://idp.test2.surfconext.nl
    //https://profile.test2.surfconext.nl/authentication/metadata is not in any stepupEntities
    private final String mockSP = "http://mock-sp";
    private final String profileSP = "https://profile.test2.surfconext.nl/authentication/metadata";

    @BeforeEach
    public void before() throws Exception {
        super.before();
        subject = new SSIDValidationHook(metaDataRepository, metaDataAutoConfiguration);
    }

    @Test
    public void appliesForMetaData() {
        assertFalse(subject.appliesForMetaData(metaData(profileSP, null, EntityType.IDP)));
        assertTrue(subject.appliesForMetaData(metaData(profileSP, "loa", EntityType.SP)));
    }

    @Test
    public void prePutSpInStepUpIdp() {
        assertThrows(ValidationException.class, () ->
                subject.prePut(null, metaData(mockSP, "loa", EntityType.SP), apiUser()));
    }

    @Test
    public void prePostSpInStepUpIdp() {
        assertThrows(ValidationException.class, () ->
                subject.prePost(metaData(mockSP, "loa", EntityType.SP), apiUser()));
    }

    @Test
    public void prePutNoLoa() {
        subject.prePut(null, metaData(mockSP, null, EntityType.SP), apiUser());
    }

    @Test
    public void prePostNoLoa() {
        subject.prePost(metaData(mockSP, null, EntityType.SP),apiUser() );
    }

    @Test
    public void prePutNotInStepUp() {
        subject.prePut(null, metaData(profileSP, "loa", EntityType.SP), apiUser());
    }

    private MetaData metaData(String spEntityId, String loa, EntityType entityType) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityid", spEntityId);
        Map<String, Object> metaDataFields = (Map<String, Object>) data.computeIfAbsent("metaDataFields", key -> new HashMap<String, Object>());
        if (StringUtils.hasText(loa)) {
            metaDataFields.put("coin:stepup:requireloa", loa);
        }
        return new MetaData(entityType.getType(), data);
    }

}