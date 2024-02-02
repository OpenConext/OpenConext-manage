package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;

import java.util.Map;

public class ExtraneousKeysPoliciesHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    public ExtraneousKeysPoliciesHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.PDP.getType());
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        removeExtraneousKeys(newMetaData);
        return super.prePut(previous, newMetaData, user);
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        removeExtraneousKeys(metaData);
        return super.prePost(metaData, user);
    }

    @SuppressWarnings("unchecked")
    private void removeExtraneousKeys(MetaData newMetaData) {
        Map<String, Object> data = newMetaData.getData();
        Map<String, Object> schemaRepresentation = this.metaDataAutoConfiguration.schemaRepresentation(EntityType.PDP);
        Map<String, Object> schemaProperties = (Map<String, Object>) schemaRepresentation.get("properties");
        //Alternative is very big refactor in IdP-Dashboard
        data.keySet().removeIf(key -> !schemaProperties.containsKey(key));
        data.put("entityid", data.get("name"));
    }

}
