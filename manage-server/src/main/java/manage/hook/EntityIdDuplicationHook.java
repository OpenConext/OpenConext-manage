package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

import java.util.ArrayList;
import java.util.List;

import static manage.model.EntityType.*;

@SuppressWarnings("unchecked")
public class EntityIdDuplicationHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;
    private final MetaDataRepository metaDataRepository;

    public EntityIdDuplicationHook(MetaDataAutoConfiguration metaDataAutoConfiguration,
                                   MetaDataRepository metaDataRepository) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return true;
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        validate(metaData, true);
        return metaData;
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        validate(newMetaData, false);
        return newMetaData;
    }

    private void validate(MetaData newMetaData, boolean isNew) {
        String entityId = this.entityId(newMetaData);
        List<EntityType> entityTypes = new ArrayList<>();
        EntityType entityType = fromType(newMetaData.getType());
        switch (entityType) {
            case EntityType.IDP:
            case EntityType.PDP:
            case EntityType.PROV:
            case EntityType.STT:
            case EntityType.ORG:
                entityTypes.add(entityType);
                break;
            case EntityType.RS:
                entityTypes.add(entityType);
                entityTypes.add(EntityType.RP);
                break;
            case EntityType.SP:
            case EntityType.SRAM:
            case EntityType.RP:
                entityTypes.addAll(List.of(EntityType.SP, EntityType.SRAM, EntityType.RP));
                break;

        }
        List<List<MetaData>> metaDataResults = entityTypes.stream()
            .map(type -> {
                String query = isNew ?
                    String.format("{\"data.entityid\": \"%s\"}", entityId) :
                    String.format("{\"data.entityid\": \"%s\",\"_id\":{$ne: \"%s\"}}", entityId, newMetaData.getId());
                return metaDataRepository.findRaw(type.getType(), query);
            }).filter(metaDataList -> !metaDataList.isEmpty())
            .toList();
        if (!metaDataResults.isEmpty()) {
            Schema schema = metaDataAutoConfiguration.schema(newMetaData.getType());
            List<ValidationException> failures = metaDataResults.stream()
                .flatMap(metaDataList -> metaDataList.stream()
                    .map(metaData -> new ValidationException(
                        metaDataAutoConfiguration.schema(metaData.getType()),
                        String.format("Duplicate entityid %s in collection %s with id %s",
                            entityId(metaData),
                            metaData.getType(),
                            metaData.getId()), null, null)))
                .toList();
            ValidationException.throwFor(schema, failures);
        }
    }

}
