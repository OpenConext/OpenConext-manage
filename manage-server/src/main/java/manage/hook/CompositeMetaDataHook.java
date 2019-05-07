package manage.hook;

import manage.model.MetaData;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
        return this.callback(metaData, (md, hook) -> hook.postGet(md));
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        AtomicReference<MetaData> ref = new AtomicReference<>(newMetaData);
        hooks.stream().filter(hook -> hook.appliesForMetaData(newMetaData)).forEach(hook -> ref.set(hook.prePut(previous, ref.get())));
        return ref.get();
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        return this.callback(metaData, (md, hook) -> hook.prePost(md));
    }

    @Override
    public MetaData preDelete(MetaData metaData) {
        return this.callback(metaData, (md, hook) -> hook.preDelete(md));
    }

    @Override
    public MetaData preValidate(MetaData metaData) {
        return this.callback(metaData, (md, hook) -> hook.preValidate(md));
    }

    private MetaData callback(MetaData metaData, Callback callback) {
        AtomicReference<MetaData> ref = new AtomicReference<>(metaData);
        hooks.stream().filter(hook -> hook.appliesForMetaData(metaData)).forEach(hook -> ref.set(callback.doHook(ref.get(), hook)));
        return ref.get();
    }

    interface Callback {
        MetaData doHook(MetaData metaData, MetaDataHook hook);
    }

}
