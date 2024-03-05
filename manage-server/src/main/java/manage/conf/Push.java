package manage.conf;

import java.io.Serializable;

public class Push implements Serializable {

    public final String url;
    public final String name;
    public final String oidcUrl;
    public final String oidcName;
    public final String pdpUrl;
    public final String pdpName;
    public final boolean excludeOidcRP;

    public Push(String url, String name, String oidcUrl, String oidcName, String pdpUrl, String pdpName, boolean excludeOidcRP) {
        this.url = url.replaceFirst("://(.*)@", "://");
        this.name = name;
        this.oidcName = oidcName;
        this.oidcUrl = oidcUrl;
        this.pdpUrl = pdpUrl;
        this.pdpName = pdpName;
        this.excludeOidcRP = excludeOidcRP;
    }
}
