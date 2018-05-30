package manage.hook;

import manage.model.MetaData;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class CompositeMetaDataHook implements MetaDataHook {

    private List<MetaDataHook> hooks;

    public CompositeMetaDataHook(List<MetaDataHook> hooks) {
        Assert.isTrue(!CollectionUtils.isEmpty(hooks), "Hooks may not be empty");
        this.hooks = hooks;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return true;
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        return hooks.stream().filter(hook -> hook.appliesForMetaData(newMetaData)).map(hook -> hook
            .prePut(previous, newMetaData)).findAny()
            .orElse(newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        return hooks.stream().filter(hook -> hook.appliesForMetaData(metaData)).map(hook -> hook
            .prePost(metaData)).findAny()
            .orElse(metaData);
    }

    @Override
    public MetaData preDelete(MetaData metaData) {
        return hooks.stream().filter(hook -> hook.appliesForMetaData(metaData)).map(hook -> hook
            .preDelete(metaData)).findAny()
            .orElse(metaData);
    }
}
