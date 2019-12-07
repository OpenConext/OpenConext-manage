package manage.model;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class ProviderIdentifierTest {

    @Test
    public void equals() {
        ProviderIdentifier p1 = getProviderIdentifier("CI", "A", "Prod");
        ProviderIdentifier p2 = getProviderIdentifier("CI", "A", "Prod");
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());


        p1 = getProviderIdentifier(null, null, "Prod");
        p2 = getProviderIdentifier(null, null, "Prod");
        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    private ProviderIdentifier getProviderIdentifier(String institutionId, String entityId, String status) {
        Map<String, Object> provider = new HashMap<>();
        Map<String, String> data = new HashMap<>();
        Map<String, String> metaDataFields = new HashMap<>();
        metaDataFields.put("coin:institution_id", institutionId);
        data.put("entityid", entityId);
        data.put("state", status);
        provider.put("data", data);
        return new ProviderIdentifier(provider);
    }
}