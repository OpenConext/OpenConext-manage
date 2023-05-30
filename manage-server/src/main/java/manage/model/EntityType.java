package manage.model;

import java.util.regex.Pattern;
import java.util.stream.Stream;

public enum EntityType {

    IDP("saml20_idp"),
    SP("saml20_sp"),
    RP("oidc10_rp", SP.getJanusDbValue()),
    RS("oauth20_rs"),
    STT("single_tenant_template", SP.getJanusDbValue()),
    PROV("provisioning");

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
        String sanitizedType = type.replaceAll(Pattern.quote("-"), "_");
        return Stream.of(EntityType.values())
                .filter(entityType -> entityType.getType().equals(sanitizedType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid EntityType " + type));
    }
}
