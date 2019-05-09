package manage.conf;

public class Push {

    public final String url;
    public final String name;
    public final String oidcUrl;
    public final String oidcName;

    public Push(String url, String name, String oidcUrl, String oidcName) {
        this.url = url.replaceFirst("://(.*)@", "://");
        this.name = name;
        this.oidcName = oidcName;
        this.oidcUrl = oidcUrl;
    }
}
