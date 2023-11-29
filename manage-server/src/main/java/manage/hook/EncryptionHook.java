package manage.hook;

import crypto.KeyStore;
import manage.model.EntityType;
import manage.model.MetaData;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

public class EncryptionHook extends MetaDataHookAdapter {

    private final KeyStore keyStore;
    private final List<String> decryptionAttributes = List.of("scim_password", "eva_token", "graph_secret");

    public EncryptionHook(KeyStore keyStore) {
        this.keyStore = keyStore;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.PROV.getType());
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        return encryptSecrets(newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        return encryptSecrets(metaData);
    }

    private MetaData encryptSecrets(MetaData newMetaData) {
        Map<String, Object> data = newMetaData.metaDataFields();
        if (!CollectionUtils.isEmpty(data)) {
            decryptionAttributes.forEach(attr -> {
                if (data.containsKey(attr)) {
                    String secret = (String) data.get(attr);
                    if (!isEncryptedSecret(secret)) {
                        try {
                            String encoded = this.keyStore.encryptAndEncode(secret);
                            data.put(attr, encoded);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            });
        }
        return newMetaData;
    }

    boolean isEncryptedSecret(String secret) {
        return this.keyStore.isEncryptedSecret(secret);
    }
}
