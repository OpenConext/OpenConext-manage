package manage.hook;

import manage.api.AbstractUser;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static manage.model.EntityType.PROV;

@SuppressWarnings("unchecked")
public class ProvisioningApplicationDeletionHook extends MetaDataHookAdapter {

    private final MetaDataRepository metaDataRepository;

    public ProvisioningApplicationDeletionHook(MetaDataRepository metaDataRepository) {
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.RP.getType()) || metaData.getType().equals(EntityType.SP.getType());
    }

    @Override
    public MetaData preDelete(MetaData metaDataToBeDeleted, AbstractUser user) {
        String id = metaDataToBeDeleted.getId();
        String metaDataType = metaDataToBeDeleted.getType();

        List<MetaData> references = metaDataRepository.findRaw(PROV.getType(),
                String.format("{$and:[{\"data.applications.id\" : \"%s\"},{\"data.applications.type\" : \"%s\"}]}",
                        id, metaDataType));

        String revisionNote = String.format("Updated after deletion of entityId %s", entityId(metaDataToBeDeleted));

        references.forEach(metaData -> {
            List<Map<String, String>> entities = (List<Map<String, String>>) metaData.getData().getOrDefault("applications", new ArrayList<>());
            entities = entities.stream().filter(entry -> !(id.equals(entry.get("id")) && metaDataType.equals(entry.get("type"))))
                    .collect(toList());
            metaData.getData().put("applications", entities);
            this.revision(metaData, revisionNote);
        });
        return metaDataToBeDeleted;
    }

    private void revision(MetaData metaData, String revisionNote) {
        String id = metaData.getId();
        MetaData previous = metaDataRepository.findById(id, metaData.getType());
        previous.revision(UUID.randomUUID().toString());
        metaDataRepository.save(previous);

        metaData.promoteToLatest("System", revisionNote);
        metaDataRepository.update(metaData);

    }

}
