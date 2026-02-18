package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;

public class OrganisationDeletionHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    private final MetaDataRepository metaDataRepository;

    private final String ORGANISATION_ID_FIELD = "organisationid";

    public OrganisationDeletionHook(MetaDataRepository metaDataRepository,
                                    MetaDataAutoConfiguration metaDataAutoConfiguration) {

        this.metaDataRepository = metaDataRepository;
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.ORG.getType());
    }

    @Override
    public MetaData preDelete(MetaData metaDataToBeDeleted, AbstractUser user) {
        String id = metaDataToBeDeleted.getId();

        if (metaDataRepository.retrieveAllEntities().stream().anyMatch(
            entity -> null != entity.getData() && null != entity.getData().get(ORGANISATION_ID_FIELD) &&
                entity.getData().get(ORGANISATION_ID_FIELD).equals(id))) {

            Schema schema = metaDataAutoConfiguration.schema(EntityType.ORG.getType());
            throw new ValidationException(
                schema,
                "Organisation cannot be deleted; it still has linked entities.", null, null);
        }

        return metaDataToBeDeleted;
    }

}
