package manage.model;

import org.springframework.util.Assert;

import java.util.regex.Pattern;

public enum EntityType {

    IDP("saml20_idp"),
    SP("saml20_sp"),
    RP("oidc10_rp", SP.getJanusDbValue());


    private final String type;
    private final String janusDbValue;

    EntityType(String type) {
        this.type = type;
        this.janusDbValue = type.replaceAll("_", "-");
    }

    EntityType(String type, String janusDbValue) {
        this.type = type;
        this.janusDbValue = janusDbValue;
    }

    public String getType() {
        return type;
    }

    public String getJanusDbValue() {
        return janusDbValue;
    }

    public static EntityType fromType(String type) {
        type = type.replaceAll(Pattern.quote("-"), "_");
        EntityType entityType = EntityType.IDP.getType().equals(type) ?
                EntityType.IDP : EntityType.SP.getType().equals(type) ? EntityType.SP :
                EntityType.RP.getType().equals(type) ? EntityType.RP : null;
        Assert.notNull(entityType, "Invalid EntityType " + type);
        return entityType;
    }
}
