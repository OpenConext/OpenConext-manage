package manage.hook;

import manage.api.AbstractUser;
import manage.exception.EndpointNotAllowed;
import manage.model.EntityType;
import manage.model.MetaData;

import java.util.Map;
import java.util.Objects;

public class SecurityHook extends MetaDataHookAdapter {

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        String type = metaData.getType();
        return type.equals(EntityType.RP.getType()) || type.equals(EntityType.SP.getType()) || type.equals(EntityType.IDP.getType());
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        return assertUserIsSuperUser(metaData, user);
    }

    @Override
    public MetaData preDelete(MetaData metaData, AbstractUser user) {
        if (user.isAPIUser() && user.isProductionEnvironment()) {
            throw new EndpointNotAllowed("API users are not allowed to delete entities");
        }
        return assertUserIsSuperUser(metaData, user);
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        Map<String, Object> previousData = previous.getData();
        Map<String, Object> newData = newMetaData.getData();
        String manipulation = (String) previousData.get("manipulation");
        String manipulationNew = (String) newData.get("manipulation");
        String manipulationNotes = (String) previousData.get("manipulationNotes");
        String manipulationNotesNew = (String) newData.get("manipulationNotes");
        boolean superUser = user.isSuperUser();
        if (!Objects.equals(manipulation, manipulationNew) && !superUser && user.isProductionEnvironment()) {
            throw new EndpointNotAllowed("Only super_users are allowed to change manipulations");
        }
        if (!Objects.equals(manipulationNotes, manipulationNotesNew) && !superUser && user.isProductionEnvironment()) {
            throw new EndpointNotAllowed("Only super_users are allowed to change manipulationNotes");
        }
        return newMetaData;
    }

    private static MetaData assertUserIsSuperUser(MetaData metaData, AbstractUser user) {
        if (metaData.getType().equals(EntityType.IDP.getType()) && !user.isSuperUser() && user.isProductionEnvironment()) {
            throw new EndpointNotAllowed("Only super_users are allowed ro create IdP's");
        }
        return metaData;
    }

}
