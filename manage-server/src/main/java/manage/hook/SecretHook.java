package manage.hook;

import manage.model.EntityType;
import manage.model.MetaData;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.CollectionUtils;

import java.util.Map;
import java.util.regex.Pattern;

public class SecretHook extends MetaDataHookAdapter {

    private final BCryptPasswordEncoder strongPasswordEncoder = new BCryptPasswordEncoder();
    private final BCryptPasswordEncoder weakerPasswordEncoder = new BCryptPasswordEncoder(5);
    private final Pattern pattern = Pattern.compile("^\\$2[ayb]\\$.{56}$");

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.RP.getType()) || metaData.getType().equals(EntityType.RS.getType());
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        return encryptSecret(newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        return encryptSecret(metaData);
    }

    private MetaData encryptSecret(MetaData newMetaData) {
        Map<String, Object> data = newMetaData.metaDataFields();
        if (!CollectionUtils.isEmpty(data) && data.containsKey("secret")) {
            String secret = (String) data.get("secret");
            if (!isBCryptEncoded(secret)) {
                String encoded = (newMetaData.getType().equals(EntityType.RP.getType())
                        ? this.strongPasswordEncoder : this.weakerPasswordEncoder).encode(secret);
                data.put("secret", encoded);
            }
        }
        return newMetaData;
    }

    boolean isBCryptEncoded(String secret) {
        return pattern.matcher(secret).matches();
    }
}
