package manage.hook;

import manage.api.AbstractUser;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static manage.model.EntityType.*;

@SuppressWarnings("unchecked")
public class StepupIdentifierReconcilerHook extends MetaDataHookAdapter {

    private final MetaDataRepository metaDataRepository;

    public StepupIdentifierReconcilerHook(MetaDataRepository metaDataRepository) {
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(STEPUP.getType());
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        String oldIdentifier = identifier(previous);
        String newIdentifier = identifier(newMetaData);

        if (oldIdentifier.equals(newIdentifier)) {
            return newMetaData;
        }
        String revisionNote = String.format("Updated after identifier rename of %s to %s", oldIdentifier, newIdentifier);

        List.of("use_ra","use_raa","select_raa").forEach(attr -> {
            List<MetaData> references = metaDataRepository.findRaw(newMetaData.getType(),
                String.format("{\"data.%s\" : \"%s\"}", attr, oldIdentifier));
            references.forEach(metaData -> {
                List<String> identifiers = (List<String>) metaData.getData().get(attr);
                identifiers.set(identifiers.indexOf(oldIdentifier), newIdentifier);
                this.revision(metaData, revisionNote);
            });
            }
        );
        newMetaData.getData().put("entityid", newIdentifier);
        return newMetaData;
    }

    @Override
    public MetaData preDelete(MetaData metaDataToBeDeleted, AbstractUser user) {
        String identifier = identifier(metaDataToBeDeleted);

        String revisionNote = String.format("Updated after deletion of identifier %s", identifier);

        List.of("use_ra","use_raa","select_raa").forEach(attr -> {
                List<MetaData> references = metaDataRepository.findRaw(metaDataToBeDeleted.getType(),
                    String.format("{\"data.%s\" : \"%s\"}", attr, identifier));
                references.forEach(metaData -> {
                    List<String> identifiers = (List<String>) metaData.getData().get(attr);
                    List<String> newIdentifiers = identifiers.stream().filter(item -> !item.equals(identifier)).toList();
                    metaData.getData().put(attr, newIdentifiers);
                    this.revision(metaData, revisionNote);
                });
            }
        );
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

    private String identifier(MetaData metaData) {
        return (String) metaData.getData().get("identifier");
    }

}
