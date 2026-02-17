package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

import java.util.List;

import static manage.model.EntityType.PDP;

@SuppressWarnings("unchecked")
public class PolicyNameConstraintsHook extends MetaDataHookAdapter {

    private final MetaDataRepository metaDataRepository;
    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    public PolicyNameConstraintsHook(MetaDataAutoConfiguration metaDataAutoConfiguration,
                                     MetaDataRepository metaDataRepository) {
        this.metaDataRepository = metaDataRepository;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.PDP.getType());
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        return doPre(metaData, true);
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        return doPre(newMetaData, false);
    }

    private MetaData doPre(MetaData newMetaData, boolean isNew) {
        String name = (String) newMetaData.getData().get("name");
        String query = "{ \"data.name\": { \"$regex\": \"^" + name + "$\", \"$options\": \"i\" } }";
        List<MetaData> metaDataList = metaDataRepository.findRaw(PDP.getType(), query);
        boolean newError = isNew && !metaDataList.isEmpty();
        boolean existingError = !isNew && metaDataList.stream()
            .anyMatch(metaData -> !metaData.getId().equals(newMetaData.getId()) &&
                ((String) metaData.getData().get("name")).equalsIgnoreCase(name));
        if (newError || existingError) {
            Schema schema = metaDataAutoConfiguration.schema(EntityType.PDP.getType());
            throw new ValidationException(schema, "Name '" + name + "' is already taken and must be unique", "name", null);
        }
        return newMetaData;
    }

}
