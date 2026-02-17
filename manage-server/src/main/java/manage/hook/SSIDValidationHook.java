package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static manage.model.EntityType.IDP;

public class SSIDValidationHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;
    private final MetaDataRepository metaDataRepository;

    public SSIDValidationHook(MetaDataRepository metaDataRepository, MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        String type = metaData.getType();
        boolean isRpOrSp = type.equals(EntityType.RP.getType()) || type.equals(EntityType.SP.getType());
        return isRpOrSp && metaData.metaDataFields().containsKey("coin:stepup:requireloa");
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
        String loa = (String) metaDataFields.get("coin:stepup:requireloa");
        if (StringUtils.hasText(loa)) {
            String entityId = (String) newMetaData.getData().get("entityid");
            List<MetaData> references = metaDataRepository.findRaw(IDP.getType(),
                    String.format("{\"data.stepupEntities.name\" : \"%s\"}", entityId));
            if (!references.isEmpty()) {
                Schema schema = metaDataAutoConfiguration.schema(newMetaData.getType());
                String msg = String.format("Not allowed to configure 'coin:stepup:requireloa'. This SP is already configured as a stepupEntity in IdP: %s",
                        references.stream().map(metaData -> (String) metaData.getData().get("entityid")).collect(Collectors.joining(", ")));
                throw new ValidationException(schema, msg, "stepupEntities", null);
            }
        }
    }
}
