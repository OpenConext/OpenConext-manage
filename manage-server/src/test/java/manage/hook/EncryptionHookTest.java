package manage.hook;

import crypto.KeyStore;
import crypto.RSAKeyStore;
import manage.TestUtils;
import manage.model.EntityType;
import manage.model.MetaData;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionHookTest implements TestUtils {

    private final KeyStore keyStore = new RSAKeyStore();
    private final EncryptionHook encryptionHook = new EncryptionHook(keyStore);;


    @Test
    void appliesForMetaData() {
        assertTrue(encryptionHook.appliesForMetaData(new MetaData(EntityType.PROV.getType(), Map.of())));
    }

    @Test
    void prePost() {
        List<String> decryptionAttributes = List.of("scim_password", "eva_token", "graph_secret");
        decryptionAttributes.forEach(attr -> {
            String plainSecret = UUID.randomUUID().toString();
            MetaData metaData = metaData(attr, plainSecret);
            metaData = encryptionHook.prePost(metaData, apiUser());
            String encrypted = (String) metaData.metaDataFields().get(attr);
            assertNotEquals(plainSecret, encrypted);
            assertTrue(keyStore.isEncryptedSecret(encrypted));

            metaData = encryptionHook.prePut(metaData, metaData, apiUser());
            String encryptedSecret = (String) metaData.metaDataFields().get(attr);
            assertEquals(encrypted, encryptedSecret);
        });

    }

    private MetaData metaData(String secret, String value) {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> metaDataFields = new HashMap<>();
        data.put("metaDataFields", metaDataFields);

        metaDataFields.put(secret, value);
        return new MetaData(EntityType.PROV.getType(), data);
    }
}