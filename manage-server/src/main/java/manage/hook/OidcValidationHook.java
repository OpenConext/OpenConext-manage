package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OidcValidationHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;
    private final boolean allowSecretPublicRP;

    public OidcValidationHook(MetaDataAutoConfiguration metaDataAutoConfiguration, boolean allowSecretPublicRP) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.allowSecretPublicRP = allowSecretPublicRP;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.RP.getType()) ||
                (metaData.getType().equals(EntityType.SRAM.getType()) &&
                        "oidc_rp".equals(metaData.metaDataFields().get("connection_type")));
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        validate(newMetaData);
        return super.prePut(previous, newMetaData, user);
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        validate(metaData);
        return super.prePost(metaData, user);
    }

    @SuppressWarnings("unchecked")
    private void validate(MetaData newMetaData) {
        Map<String, Object> metaDataFields = newMetaData.metaDataFields();
        List<String> redirectUrls = (List<String>) metaDataFields.getOrDefault("redirectUrls", new ArrayList<String>());
        List<String> grants = (List<String>) metaDataFields.getOrDefault("grants", new ArrayList<String>());
        boolean isPublicClient = (boolean) metaDataFields.getOrDefault("isPublicClient", false);
        String secret = (String) metaDataFields.get("secret");
        Schema schema = metaDataAutoConfiguration.schema(EntityType.RP.getType());
        List<ValidationException> failures = new ArrayList<>();
        if (grants.stream().anyMatch(grant -> Arrays.asList("authorization_code", "implicit").contains(grant)) &&
                redirectUrls.isEmpty()) {
            failures.add(new ValidationException(schema, "Redirect URI is required with selected grant types", "redirectUris", null));
        }
        if (grants.size() == 1 && grants.get(0).equals("client_credentials") && redirectUrls.size() > 0) {
            failures.add(new ValidationException(schema, "Redirect URI is not allowed with selected grant type", "redirectUris", null));
        }
        if (grants.size() == 1 && grants.get(0).equals("refresh_token")) {
            failures.add(new ValidationException(schema, "Refresh token grant must be combined with another grant type", "grants", null));
        }
        if (metaDataFields.get("refreshTokenValidity") != null && grants.stream().noneMatch(grant -> grant.equals("refresh_token"))) {
            failures.add(new ValidationException(schema, "refreshTokenValidity specified, but no refresh_token grant. Either remove refreshTokenValidity or add refresh_token grant type", "refreshTokenValidity", null));
        }
        if (!allowSecretPublicRP) {
            if (isPublicClient && StringUtils.hasText(secret)) {
                failures.add(new ValidationException(schema, "Public clients are not allowed a secret", "isPublicClient", null));
            }
            if (!isPublicClient && !StringUtils.hasText(secret)) {
                failures.add(new ValidationException(schema, "Non-public clients are required a secret", "secret", null));
            }
            if (grants.size() == 1 && grants.get(0).equals("urn:ietf:params:oauth:grant-type:device_code") && StringUtils.hasText(secret)) {
                failures.add(new ValidationException(schema, "Device Code RP is not allowed a secret", "redirectUris", null));
            }
        }
        ValidationException.throwFor(schema, failures);
    }

}
