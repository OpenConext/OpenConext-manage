package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EmptyRevisionHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    private final List<String> ignoreInDiff = Arrays.asList("revisionnote");

    public EmptyRevisionHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        Map<String, Object> previousData = previous.getData();
        Map<String, Object> newData = newMetaData.getData();
        boolean eq = mapEquality(previousData, newData) && mapEquality(newData, previousData);
        if (eq) {
            //we need a schema, does not matter for which entityType
            Schema schema = metaDataAutoConfiguration.schema(EntityType.RP.getType());
            throw new ValidationException(schema, "No data is changed. An update would result in an empty revision", "empty-revision");
        }
        return super.prePut(previous, newMetaData, user);
    }

    private boolean mapEquality(Map<String, Object> first, Map<String, Object> second) {
        return first.entrySet().stream()
                .allMatch(e -> {
                    String firstKey = e.getKey();
                    Object firstValue = e.getValue();
                    Object secondValue = second.get(firstKey);

                    return ignoreInDiff.contains(firstKey) ||
                            (firstValue == null && secondValue == null) ||
                            (firstValue != null && firstValue.equals(secondValue));
                });
    }
}
