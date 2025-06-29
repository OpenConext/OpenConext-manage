package manage.hook;

import manage.api.AbstractUser;
import manage.model.MetaData;

public interface MetaDataHook {

    boolean appliesForMetaData(MetaData metaData);

    MetaData postGet(MetaData metaData);

    MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user);

    MetaData prePost(MetaData metaData, AbstractUser user);

    MetaData preDelete(MetaData metaData, AbstractUser user);

    MetaData preValidate(MetaData metaData);

    default String entityId(MetaData metaData) {
        return (String) metaData.getData().get("entityid");
    }


}
