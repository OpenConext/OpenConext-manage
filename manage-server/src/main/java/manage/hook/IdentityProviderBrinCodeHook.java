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

public class IdentityProviderBrinCodeHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    public IdentityProviderBrinCodeHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.IDP.getType());
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
        String brinCode = (String) metaDataFields.get("coin:institution_brin");
        String brinCodeSchacHome = (String) metaDataFields.get("coin:institution_brin_schac_home");
        List<ValidationException> failures = new ArrayList<>();
        Schema schema = metaDataAutoConfiguration.schema(EntityType.IDP.getType());
        if (StringUtils.hasText(brinCode) && !StringUtils.hasText(brinCodeSchacHome)) {
            failures.add(new ValidationException(schema, "coin:institution_brin_schac_home is required for an IdP with coin:institution_brin",
                "coin:institution_brin_schac_home", null));
        }
        if (!StringUtils.hasText(brinCode) && StringUtils.hasText(brinCodeSchacHome)) {
            failures.add(new ValidationException(schema, "coin:institution_brin is required for an IdP with coin:institution_brin_schac_home",
                "coin:institution_brin", null));
        }
        ValidationException.throwFor(schema, failures);
    }

}
