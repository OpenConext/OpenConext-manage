package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.shibboleth.FederatedUser;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class PolicyValidationHook extends MetaDataHookAdapter {

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    public PolicyValidationHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.PDP.getType());
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
        Schema schema = metaDataAutoConfiguration.schema(EntityType.PDP.getType());
        Map<String, Object> data = newMetaData.getData();
        List<ValidationException> failures = new ArrayList<>();
        boolean isRegularPolicy = "reg".equals(data.get("type"));
        if (isRegularPolicy) {
            if (!StringUtils.hasText((String) data.get("denyAdviceNl"))) {
                failures.add(new ValidationException(schema, "Deny advice Nl is required for regular policies", "denyAdviceNl"));
            }
            if (!StringUtils.hasText((String) data.get("denyAdvice"))) {
                failures.add(new ValidationException(schema, "Deny advice is required for regular policies", "denyAdvice"));
            }
            List<Map<String, Object>> attributes = (List<Map<String, Object>>) data.get("attributes");
            if (CollectionUtils.isEmpty(attributes) || attributes.get(0).isEmpty()) {
                failures.add(new ValidationException(schema, "One or more attributes are required for regular policies", "attributes"));
            }
        } else {
            List<Map<String, Object>> loas = (List<Map<String, Object>>) data.get("loas");
            if (CollectionUtils.isEmpty(loas) || loas.get(0).isEmpty()) {
                failures.add(new ValidationException(schema, "One or more level of assurances are required for regular policies", "loas"));
            }
        }
        ValidationException.throwFor(schema, failures);
    }

}
