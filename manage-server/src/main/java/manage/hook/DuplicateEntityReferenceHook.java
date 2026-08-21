package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

@SuppressWarnings("unchecked")
public class DuplicateEntityReferenceHook extends MetaDataHookAdapter {

    private static final Map<EntityType, List<String>> ATTRIBUTES_TO_CHECK = Map.of(
        EntityType.IDP, List.of("allowedEntities", "stepupEntities", "mfaEntities", "disableConsent"),
        EntityType.SP, List.of("allowedEntities"),
        EntityType.RP, List.of("allowedEntities", "allowedResourceServers"),
        EntityType.STT, List.of("allowedEntities"),
        EntityType.SRAM, List.of("allowedEntities"),
        EntityType.PDP, List.of("serviceProviderIds", "identityProviderIds"));

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    public DuplicateEntityReferenceHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return ATTRIBUTES_TO_CHECK.containsKey(EntityType.fromType(metaData.getType()));
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        validate(newMetaData);
        return super.prePut(previous, newMetaData, user);
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        validate(metaData);
        return super.prePost(metaData, user);
    }

    private void validate(MetaData metaData) {
        List<ValidationException> failures = new ArrayList<>();
        List<String> attributesToCheck = ATTRIBUTES_TO_CHECK.getOrDefault(
            EntityType.fromType(metaData.getType()), List.of());
        attributesToCheck.forEach(attribute -> {
            if (metaData.getData().containsKey(attribute)) {
                List<Map<String, String>> references = (List<Map<String, String>>) metaData.getData().get(attribute);
                if (references != null) {
                    Map<String, Long> countByName = references.stream()
                        .map(reference -> reference.get("name"))
                        .collect(groupingBy(name -> name, counting()));
                    countByName.forEach((name, count) -> {
                        if (count > 1) {
                            failures.add(new ValidationException(
                                metaDataAutoConfiguration.schema(metaData.getType()),
                                String.format("Duplicate name '%s' in attribute '%s'", name, attribute),
                                attribute, null));
                        }
                    });
                }
            }
        });
        if (!failures.isEmpty()) {
            Schema schema = metaDataAutoConfiguration.schema(metaData.getType());
            ValidationException.throwFor(schema, failures);
        }
    }

}
