package manage.conf;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Product {

    public static final Product DEFAULT =
            new Product("OpenConext", "Manage", "http://mds.edugain.org/");

    public final String organization;
    public final String name;
    public final String serviceProviderFeedUrl;

}
