package manage.hook;

import manage.api.AbstractUser;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static manage.model.EntityType.IDP;
import static manage.model.EntityType.RP;
import static manage.model.EntityType.RS;
import static manage.model.EntityType.SP;

@SuppressWarnings("unchecked")
public class EntityIdConstraintsHook extends MetaDataHookAdapter {

    private MetaDataRepository metaDataRepository;

    public EntityIdConstraintsHook(MetaDataRepository metaDataRepository) {
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return !metaData.getType().equals(EntityType.STT.getType()) && !metaData.getType().equals(EntityType.PROV.getType());
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        return doPre(metaData);
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        return doPre(newMetaData);
    }

    private MetaData doPre(MetaData newMetaData) {
        String metaDataType = newMetaData.getType();
        Map<String, List<EntityType>> relationsToCheck = new HashMap<>();

        List<EntityType> reversedEntityType = metaDataType.equals(IDP.getType()) ? Arrays.asList(SP, RP) : singletonList(IDP);
        relationsToCheck.put("allowedEntities", reversedEntityType);
        relationsToCheck.put("disableConsent", reversedEntityType);
        relationsToCheck.put("stepupEntities", Arrays.asList(SP, RP));
        relationsToCheck.put("mfaEntities", Arrays.asList(SP, RP));
        relationsToCheck.put("allowedResourceServers", singletonList(RS));

        relationsToCheck.forEach((key, value) -> {
            if (newMetaData.getData().containsKey(key)) {
                List<Map<String, String>> references = (List<Map<String, String>>) newMetaData.getData().get(key);
                if (!CollectionUtils.isEmpty(references)) {
                    List<Map<String, String>> strippedReferences = references.stream()
                            .filter(map -> value.stream()
                                    .anyMatch(entityType ->
                                            !CollectionUtils.isEmpty(
                                                    metaDataRepository.findRaw(entityType.getType(),
                                                            String.format("{\"data.entityid\" : \"%s\"}", map.get("name")))))
                            ).collect(toList());
                    newMetaData.getData().put(key, strippedReferences);
                }
            }
        });
        return newMetaData;
    }

}
