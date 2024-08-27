package manage.hook;

import crypto.KeyStore;
import manage.api.AbstractUser;
import manage.model.EntityType;
import manage.model.MetaData;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;

public class EncryptionHook extends MetaDataHookAdapter {

    private final KeyStore keyStore;
    private final boolean cryptoEnabled;
    private final List<String> decryptionAttributes = List.of("scim_password", "eva_token", "graph_secret");

    public EncryptionHook(KeyStore keyStore, boolean cryptoEnabled) {
        this.keyStore = keyStore;
        this.cryptoEnabled = cryptoEnabled;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.PROV.getType()) && cryptoEnabled;
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        return encryptSecrets(newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
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

    private boolean isEncryptedSecret(String secret) {
        return this.keyStore.isEncryptedSecret(secret);
    }
}
