package manage.hook;

import manage.AbstractIntegrationTest;
import manage.api.APIUser;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

public class ProvisioningHookTest extends AbstractIntegrationTest {

    private ProvisioningHook provisioningHook;
    private final APIUser apiUser = new APIUser("test", emptyList());


    @Before
    public void before() throws Exception {
        super.before();
        provisioningHook = new ProvisioningHook(metaDataRepository, metaDataAutoConfiguration);
    }

    @Test
    public void appliesForMetaData() {
        assertTrue(provisioningHook.appliesForMetaData(new MetaData(EntityType.PROV.getType(), Map.of())));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void prePut() {
        MetaData previous = new MetaData(EntityType.PROV.getType(), new HashMap<>(Map.of("metaDataFields",
                Map.of("provisioning_type", "scim",
                        "scim_url", "https://scim.url",
                        "scim_user", "scim_user",
                        "scim_password", "secret"))));
        MetaData newMetaData = new MetaData(EntityType.PROV.getType(), new HashMap<>(Map.of("metaDataFields",
                Map.of("provisioning_type", "eva",
                        "eva_url", "https://eva.url",
                        "eva_token", "token"))));
        assertThrows(ValidationException.class, () -> provisioningHook.prePut(previous, newMetaData, apiUser));

        MetaData validNewMetaData = new MetaData(EntityType.PROV.getType(), new HashMap<>(previous.getData()));
        validNewMetaData.getData().put("applications", List.of(Map.of("id", "nope", "type", EntityType.PROV.getType())));
        MetaData metaData = provisioningHook.prePut(previous, validNewMetaData, apiUser);
        assertEquals(0, ((List) metaData.getData().get("applications")).size());
    }

    @Test
    public void prePost() {
        MetaData metaData = new MetaData(EntityType.PROV.getType(), new HashMap<>(Map.of("metaDataFields",
                Map.of("provisioning_type", "scim"))));
        assertThrows(ValidationException.class, () -> provisioningHook.prePost(metaData, apiUser));
    }
}