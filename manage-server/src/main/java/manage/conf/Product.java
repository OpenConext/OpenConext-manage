package manage.conf;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Product {

    public final String organization;
    public final String name;

    public static final Product DEFAULT = new Product("OpenConext","Manage");

}
