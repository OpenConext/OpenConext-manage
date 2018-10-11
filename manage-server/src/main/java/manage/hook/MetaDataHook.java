package manage.hook;

import manage.model.MetaData;

public interface MetaDataHook {

    boolean appliesForMetaData(MetaData metaData);

    MetaData postGet(MetaData metaData);

    MetaData prePut(MetaData previous, MetaData newMetaData);

    MetaData prePost(MetaData metaData);

    MetaData preDelete(MetaData metaData);

}
