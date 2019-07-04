package manage.hook;

import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static manage.model.EntityType.IDP;
import static manage.model.EntityType.RP;
import static manage.model.EntityType.SP;

@SuppressWarnings("unchecked")
public class EntityIdConstraintsHook extends MetaDataHookAdapter {

    private MetaDataRepository metaDataRepository;

    public EntityIdConstraintsHook(MetaDataRepository metaDataRepository) {
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return true;
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        return doPre(metaData);
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        return doPre(newMetaData);
    }

    private MetaData doPre(MetaData newMetaData) {
        String metaDataType = newMetaData.getType();
        Map<String, EntityType> relationsToCheck = new HashMap<>();

        relationsToCheck.put("allowedEntities", metaDataType.equals(SP.getType()) || metaDataType.equals(RP.getType()) ? IDP : SP);
        relationsToCheck.put("disableConsent", metaDataType.equals(SP.getType()) || metaDataType.equals(RP.getType()) ? IDP : SP);
        relationsToCheck.put("allowedResourceServers", RP);

        relationsToCheck.entrySet().forEach(entry -> {
            String key = entry.getKey();
            if (newMetaData.getData().containsKey(key)) {
                List<Map<String, String>> references = (List<Map<String, String>>) newMetaData.getData().get(key);
                if (!CollectionUtils.isEmpty(references)) {
                    List<Map<String, String>> strippedReferences = references.stream()
                            .filter(map ->
                                    !CollectionUtils.isEmpty(metaDataRepository.findRaw(entry.getValue().getType(),
                                            String.format("{\"data.entityid\" : \"%s\"}", map.get("name")))))
                            .collect(toList());
                    newMetaData.getData().put(key, strippedReferences);
                }
            }
        });
        return newMetaData;
    }

}
