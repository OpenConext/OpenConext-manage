package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class IdentityProviderDeleteHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;
    private final MetaDataRepository metaDataRepository;

    public IdentityProviderDeleteHook(MetaDataAutoConfiguration metaDataAutoConfiguration,
                                      MetaDataRepository metaDataRepository) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.IDP.getType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public MetaData preDelete(MetaData metaData, AbstractUser user) {
        String entityId = entityId(metaData);
        String query = String.format("{\"data.identityProviderIds.name\":\"%s\"}", entityId);
        List<MetaData> policies = metaDataRepository.findRaw(EntityType.PDP.getType(), query);
        if (!policies.isEmpty()) {
            Schema schema = metaDataAutoConfiguration.schema(EntityType.PDP.getType());
            List<ValidationException> exceptions = policies.stream().map(policy ->
                new ValidationException(
                    schema, String.format("The policy %s uses this IdP. First remove this IdP from the policy.",
                    policy.getData().get("name")), null, null
                )).toList();
            ValidationException.throwFor(schema, exceptions);
        }
        return metaData;
    }

}
