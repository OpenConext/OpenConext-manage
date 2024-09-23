package manage.policies;

import lombok.Getter;
import lombok.Setter;

@Getter
public class PolicySummary {

    private final String name;
    private final String xml;
    private final boolean active;
    @Setter
    private String description;

    public PolicySummary(String name, String xml, boolean active) {
        this.name = name;
        this.xml = xml;
        this.active = active;
    }

}
