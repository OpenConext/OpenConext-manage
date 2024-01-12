package manage.api;

public interface AbstractUser {
    String getName();

    boolean isAPIUser();

    boolean isSystemUser();

    boolean isAllowed(Scope... scopes);
}
