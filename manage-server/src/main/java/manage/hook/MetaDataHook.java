package manage.hook;

import manage.model.MetaData;
import manage.model.MetaDataUpdate;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static manage.model.EntityType.IDP;
import static manage.model.EntityType.RP;
import static manage.model.EntityType.SP;

public interface MetaDataHook {

    boolean appliesForMetaData(MetaData metaData);

    MetaData postGet(MetaData metaData);

    MetaData prePut(MetaData previous, MetaData newMetaData);

    MetaData prePost(MetaData metaData);

    MetaData preDelete(MetaData metaData);

    MetaData preValidate(MetaData metaData);

}
