package manage.hook;

import lombok.Getter;
import lombok.Setter;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;

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
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        return newMetaData;
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        return metaData;
    }

    @Override
    public MetaData preDelete(MetaData metaData) {
        return metaData;
    }
}
