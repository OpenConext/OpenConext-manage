package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

import java.util.List;

public class ServiceProviderDeleteHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;
    private final MetaDataRepository metaDataRepository;

    public ServiceProviderDeleteHook(MetaDataAutoConfiguration metaDataAutoConfiguration,
                                     MetaDataRepository metaDataRepository) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.RP.getType()) || metaData.getType().equals(EntityType.SP.getType());
    }

    @Override
    @SuppressWarnings("unchecked")
    public MetaData preDelete(MetaData metaData, AbstractUser user) {
        String entityId = entityId(metaData);
        String query = String.format("{\"data.serviceProviderIds.name\":\"%s\"}", entityId);
        List<MetaData> policies = metaDataRepository
            .findRaw(EntityType.PDP.getType(), query)
            .stream()
            //Allow for the deletion if there are other service providers in the policy
            .filter(md -> ((List) md.getData().get("serviceProviderIds")).size() == 1)
            .toList();
        if (!policies.isEmpty()) {
            Schema schema = metaDataAutoConfiguration.schema(EntityType.PDP.getType());
            List<ValidationException> exceptions = policies.stream().map(policy ->
                new ValidationException(
                    schema, String.format("The policy %s uses this %s. First remove this %s from the policy.",
                    policy.getData().get("name"),
                    metaData.getType(),
                    metaData.getType())
                )).toList();
            ValidationException.throwFor(schema, exceptions);
        }
        return metaData;
    }

}
