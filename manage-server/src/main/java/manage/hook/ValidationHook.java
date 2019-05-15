package manage.hook;

import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ValidationHook extends MetaDataHookAdapter {

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    public ValidationHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals("oidc10_rp");
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        validate(newMetaData);
        return super.prePut(previous, newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        validate(metaData);
        return super.prePost(metaData);
    }

    @SuppressWarnings("unchecked")
    private void validate(MetaData newMetaData) {
        Map<String, Object> metaDataFields = newMetaData.metaDataFields();
        List<String> redirectUrls = (List<String>) metaDataFields.getOrDefault("redirectUrls", new ArrayList<String>());
        List<String> grants = (List<String>) metaDataFields.getOrDefault("grants", new ArrayList<String>());
        if (grants.stream().anyMatch(grant -> Arrays.asList("authorization_code", "implicit").contains(grant)) &&
                redirectUrls.isEmpty()) {
            Schema schema = metaDataAutoConfiguration.schema(EntityType.RP.getType());
            throw new ValidationException(schema, "Redirect URI is required with selected grant types", "redirectUris");
        }
    }

}
