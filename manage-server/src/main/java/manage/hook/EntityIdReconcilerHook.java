package manage.hook;

import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("unchecked")
public class EntityIdReconcilerHook extends MetaDataHookAdapter {

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return EntityType.SP.getType().equals(metaData.getType()) || EntityType.IDP.getType().equals(metaData.getType
            ());
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        String oldEntityId = entityId(previous);
        String newEntityId = entityId(newMetaData);

        if (oldEntityId.equals(newEntityId)) {
            return newMetaData;
        }

        MetaDataRepository repository = getMetaDataRepository();
        Arrays.asList("allowedEntities", "disableConsent").forEach(name -> {
            List<MetaData> references = repository.findRaw(oppositeProviderType(newMetaData),
                String.format("{\"data.%s.name\" : \"%s\"}", name, oldEntityId));

            String revisionNote = String.format("Updated after entityId rename of %s to %s", oldEntityId, newEntityId);

            references.forEach(metaData -> {
                List<Map<String, String>> entities = (List<Map<String, String>>) metaData.getData().get(name);
                entities.stream().filter(entry -> oldEntityId.equals(entry.get("name")))
                    .findAny()
                    .ifPresent(entry -> entry.put("name", newEntityId));
                metaData.getData().put("revisionnote", revisionNote);
                this.revision(repository, metaData);
            });
        });
        return newMetaData;
    }

    @Override
    public MetaData preDelete(MetaData metaDataToBeDeleted) {
        MetaDataRepository repository = getMetaDataRepository();
        String entityId = entityId(metaDataToBeDeleted);
        Arrays.asList("allowedEntities", "disableConsent").forEach(name -> {
            List<MetaData> references = repository.findRaw(oppositeProviderType(metaDataToBeDeleted),
                String.format("{\"data.%s.name\" : \"%s\"}", name, entityId));

            String revisionNote = String.format("Updated after deletion of entityId %s", entityId);

            references.forEach(metaData -> {
                List<Map<String, String>> entities = (List<Map<String, String>>) metaData.getData().get(name);
                entities = entities.stream().filter(entry -> !entityId.equals(entry.get("name"))).collect(toList());
                metaData.getData().put(name, entities);
                metaData.getData().put("revisionnote", revisionNote);
                this.revision(repository, metaData);
            });
        });
        return metaDataToBeDeleted;
    }

    private String oppositeProviderType(MetaData metaData) {
        String type = metaData.getType();
        if (type.equals(EntityType.SP.getType())) {
            return EntityType.IDP.getType();
        }
        if (type.equals(EntityType.IDP.getType())) {
            return EntityType.SP.getType();
        }
        throw new IllegalArgumentException("Not supported MetaData type " + type);
    }

    private String entityId(MetaData metaData) {
        return (String) metaData.getData().get("entityid");
    }

    private void revision(MetaDataRepository metaDataRepository, MetaData metaData) {
        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());
        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest("System");
        metaDataRepository.update(metaData);

    }
}
