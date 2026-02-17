package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.regex.Pattern;

public class SecretHook extends MetaDataHookAdapter {

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(5);
    private final Pattern pattern = Pattern.compile("^\\$2[ayb]\\$.{56}$");
    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    public SecretHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.RP.getType()) ||
                metaData.getType().equals(EntityType.RS.getType()) ||
                metaData.getType().equals(EntityType.SRAM.getType());
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        return encryptSecret(newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        return encryptSecret(metaData);
    }

    private MetaData encryptSecret(MetaData newMetaData) {
        Map<String, Object> data = newMetaData.metaDataFields();
        if (!CollectionUtils.isEmpty(data) && data.containsKey("secret")) {
            String secret = (String) data.get("secret");
            if (!isBCryptEncoded(secret)) {
                if (secret == null || secret.trim().length() < 12) {
                    Schema schema = metaDataAutoConfiguration.schema(newMetaData.getType());
                    throw new ValidationException(schema, "Secret has minimal length of 12 characters", "metaDataFields.secret", null);
                } else {
                    String encoded = this.passwordEncoder.encode(secret);
                    data.put("secret", encoded);
                }
            }
        }
        return newMetaData;
    }

    boolean isBCryptEncoded(String secret) {
        return pattern.matcher(secret).matches();
    }
}
