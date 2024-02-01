package manage.hook;

import manage.api.AbstractUser;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static manage.model.EntityType.*;

@SuppressWarnings("unchecked")
public class EntityIdReconcilerHook extends MetaDataHookAdapter {

    private final MetaDataRepository metaDataRepository;

    public EntityIdReconcilerHook(MetaDataRepository metaDataRepository) {
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return !metaData.getType().equals(EntityType.STT.getType()) && !metaData.getType().equals(EntityType.PROV.getType());
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        String oldEntityId = entityId(previous);
        String newEntityId = entityId(newMetaData);

        if (oldEntityId.equals(newEntityId)) {
            return newMetaData;
        }
        String metaDataType = newMetaData.getType();
        List<String> types = metaDataTypesForeignKeyRelations(metaDataType);
        getCollectionReferenceNames(metaDataType).forEach(name ->
                types.forEach(type -> {
                    List<MetaData> references = metaDataRepository.findRaw(type,
                            String.format("{\"data.%s.name\" : \"%s\"}", name, oldEntityId));

                    String revisionNote = String.format("Updated after entityId rename of %s to %s", oldEntityId, newEntityId);

                    references.forEach(metaData -> {
                        List<Map<String, String>> entities = (List<Map<String, String>>) metaData.getData().getOrDefault(name, new ArrayList<>());
                        entities.stream()
                                .filter(entry -> oldEntityId.equals(entry.get("name")))
                                .findAny()
                                .ifPresent(entry -> entry.put("name", newEntityId));
                        this.revision(metaData, revisionNote);
                    });
                }));
        return newMetaData;
    }

    @Override
    public MetaData preDelete(MetaData metaDataToBeDeleted, AbstractUser user) {
        String entityId = entityId(metaDataToBeDeleted);
        String metaDataType = metaDataToBeDeleted.getType();

        List<String> types = metaDataTypesForeignKeyRelations(metaDataType);
        getCollectionReferenceNames(metaDataType).forEach(name -> {
            types.forEach(type -> {
                List<MetaData> references = metaDataRepository.findRaw(type,
                        String.format("{\"data.%s.name\" : \"%s\"}", name, entityId));

                String revisionNote = String.format("Updated after deletion of entityId %s", entityId);

                references.forEach(metaData -> {
                    List<Map<String, String>> entities = (List<Map<String, String>>) metaData.getData().getOrDefault(name, new ArrayList<>());
                    entities = entities.stream().filter(entry -> !entityId.equals(entry.get("name"))).collect(toList());
                    metaData.getData().put(name, entities);
                    this.revision(metaData, revisionNote);
                });
            });
        });
        return metaDataToBeDeleted;
    }

    private String entityId(MetaData metaData) {
        return (String) metaData.getData().get("entityid");
    }

    private void revision(MetaData metaData, String revisionNote) {
        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());
        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest("System", revisionNote);
        metaDataRepository.update(metaData);

    }
    private List<String> getCollectionReferenceNames(String type) {
        if (type.equals(STT.getType())) {
            return emptyList();
        }
        if (type.equals(SP.getType()) || type.equals(RP.getType())) {
            return asList("allowedEntities", "stepupEntities", "mfaEntities", "disableConsent");
        }
        if (type.equals(IDP.getType())) {
            return singletonList("allowedEntities");
        }
        if (type.equals(RS.getType())) {
            return singletonList("allowedResourceServers");
        }
        if (type.equals(PDP.getType())) {
            return asList("serviceProviderIds", "identityProviderIds");
        }
        return Collections.emptyList();
    }


    public static List<String> metaDataTypesForeignKeyRelations(String type) {
        if (type.equals(STT.getType())) {
            return emptyList();
        }
        if (type.equals(SP.getType()) || type.equals(RP.getType())) {
            return singletonList(IDP.getType());
        }
        if (type.equals(IDP.getType())) {
            return asList(SP.getType(), RP.getType());
        }
        if (type.equals(RS.getType())) {
            return singletonList(RP.getType());
        }
        if (type.equals(PDP.getType())) {
            return asList(IDP.getType(), SP.getType(), RP.getType());
        }
        return Collections.emptyList();
    }

}
