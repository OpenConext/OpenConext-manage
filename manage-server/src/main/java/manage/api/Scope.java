package manage.api;

public enum Scope {

    CHANGE_REQUEST, //Allowed to create change requests
    PUSH, //Allowed to push changes to EB & OIDC-NG
    READ, //Allowed to read entities
    SYSTEM, //Allowed everything including Attribute Manipulation and updating / deleting Identity Providers
    TEST, //Only used internally
    WRITE, //Allowed to create (excluding Identity Providers) and update all entities
    DELETE //Allowed to delete entities (excluding Identity Providers)

}
