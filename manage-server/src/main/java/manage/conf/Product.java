package manage.conf;

import lombok.AllArgsConstructor;

import java.io.Serializable;

@AllArgsConstructor
public class Product implements Serializable {

    public final String organization;
    public final String name;
    public final String serviceProviderFeedUrl;
    public final String jiraBaseUrl;
    public final boolean showOidcRp;

}
