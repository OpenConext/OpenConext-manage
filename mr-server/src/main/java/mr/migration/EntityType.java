package mr.migration;

public enum EntityType {

    IDP("saml20_idp"),
    SP("saml20_sp");

    private final String type;
    private final String janusDbValue;

    EntityType(String type) {
        this.type = type;
        this.janusDbValue = type.replaceAll("_", "-");
    }

    public String getType() {
        return type;
    }

    public String getJanusDbValue() {
        return janusDbValue;
    }
}
