package manage.conf;

import org.springframework.beans.factory.annotation.Value;

public class Push {

    public final String url;
    public final String name;
    public final String oidcUrl;
    public final String oidcName;
    public final boolean excludeOidcRP;

    public Push(String url, String name, String oidcUrl, String oidcName, boolean excludeOidcRP) {
        this.url = url.replaceFirst("://(.*)@", "://");
        this.name = name;
        this.oidcName = oidcName;
        this.oidcUrl = oidcUrl;
        this.excludeOidcRP = excludeOidcRP;
    }
}
