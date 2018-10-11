package manage.hook;

import manage.model.MetaData;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
    public MetaData postGet(MetaData metaData) {
        AtomicReference<MetaData> ref = new AtomicReference<>(metaData);
        hooks.stream().filter(hook -> hook.appliesForMetaData(metaData)).forEach(hook -> ref.set(hook.postGet(ref.get())));
        return ref.get();
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        AtomicReference<MetaData> ref = new AtomicReference<>(newMetaData);
        hooks.stream().filter(hook -> hook.appliesForMetaData(newMetaData)).forEach(hook -> ref.set(hook.prePut(previous, ref.get())));
        return ref.get();
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        AtomicReference<MetaData> ref = new AtomicReference<>(metaData);
        hooks.stream().filter(hook -> hook.appliesForMetaData(metaData)).forEach(hook -> ref.set(hook.prePost(ref.get())));
        return ref.get();
    }

    @Override
    public MetaData preDelete(MetaData metaData) {
        AtomicReference<MetaData> ref = new AtomicReference<>(metaData);
        hooks.stream().filter(hook -> hook.appliesForMetaData(metaData)).forEach(hook -> ref.set(hook.preDelete(ref.get())));
        return ref.get();
    }

}
