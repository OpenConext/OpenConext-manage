package manage.conf;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Product {

    public final String organization;
    public final String name;
    public final String serviceProviderFeedUrl;
    public final boolean showOidcRp;

}
