package manage.hook;

import manage.api.AbstractUser;
import manage.api.Scope;
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
        validateIdPScope(metaData, user);
        return metaData;
    }

    @Override
    public MetaData preDelete(MetaData metaData, AbstractUser user) {
        validateIdPScope(metaData, user);
        return metaData;
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        validateIdPScope(newMetaData, user);
        Map<String, Object> previousData = previous.getData();
        Map<String, Object> newData = newMetaData.getData();
        String manipulation = (String) previousData.get("manipulation");
        String manipulationNew = (String) newData.get("manipulation");
        String manipulationNotes = (String) previousData.get("manipulationNotes");
        String manipulationNotesNew = (String) newData.get("manipulationNotes");
        boolean systemUser = user.isSystemUser();
        if (!Objects.equals(manipulation, manipulationNew) && !systemUser) {
            throw new EndpointNotAllowed(String.format("User %s is not allowed to change manipulations", user.getName()));
        }
        if (!Objects.equals(manipulationNotes, manipulationNotesNew) && !systemUser) {
            throw new EndpointNotAllowed(String.format("User %s is not allowed to change manipulationNotes", user.getName()));
        }
        return newMetaData;
    }

    private static void validateIdPScope(MetaData metaData, AbstractUser user) {
        if (metaData.getType().equals(EntityType.IDP.getType()) && !user.isAllowed(Scope.WRITE_IDP) && !user.isAllowed(Scope.ADMIN) && !user.isSystemUser()) {
            throw new EndpointNotAllowed(String.format("APIUser %s is not allowed to create IdP's", user.getName()));
        }
    }


}
