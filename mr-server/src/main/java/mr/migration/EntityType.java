package mr.migration;

public enum EntityType {

  IDP("saml20-idp"),
  SP("saml20-sp");

  private final String type;

  EntityType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }
}
