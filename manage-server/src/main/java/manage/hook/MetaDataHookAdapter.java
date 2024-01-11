package manage.hook;

import manage.api.AbstractUser;
import manage.model.MetaData;

public class MetaDataHookAdapter implements MetaDataHook {

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return true;
    }

    @Override
    public MetaData postGet(MetaData metaData) {
        return metaData;
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        return newMetaData;
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        return metaData;
    }

    @Override
    public MetaData preDelete(MetaData metaData, AbstractUser user) {
        return metaData;
    }

    @Override
    public MetaData preValidate(MetaData metaData) {
        return metaData;
    }
}
