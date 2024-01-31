package manage.policies;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import manage.model.MetaData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("unchecked")
public class PdpPolicyDefinition {

    private String id;
    private String name;
    private String description;

    private List<String> serviceProviderIds = new ArrayList<>();
    private List<String> serviceProviderNames = new ArrayList<>();
    private List<String> serviceProviderNamesNl = new ArrayList<>();

    private List<String> identityProviderIds = new ArrayList<>();
    private List<String> identityProviderNames = new ArrayList<>();
    private List<String> identityProviderNamesNl = new ArrayList<>();

    private String clientId;

    private List<PdpAttribute> attributes = new ArrayList<>();

    private List<LoA> loas = new ArrayList<>();

    private boolean denyRule;

    private boolean allAttributesMustMatch;

    private Date created;

    private String userDisplayName;

    private String authenticatingAuthorityName;

    private String denyAdvice;

    private String denyAdviceNl;

    private boolean isActivatedSr;

    private boolean active;

    private boolean actionsAllowed;

    private String type;

    public PdpPolicyDefinition(MetaData metaData) {
        this.id = metaData.getId();
        Map<String, Object> data = metaData.getData();
        this.name = (String) data.get("name");
        this.description = (String) data.get("description");
        this.serviceProviderIds = (List<String>) data.get("serviceProviderIds");
        this.identityProviderIds = (List<String>) data.getOrDefault("identityProviderIds", new ArrayList<>());
        this.attributes = ((List<Map<String, String>>) data.getOrDefault("attributes", new ArrayList<>())).stream()
                .map(m -> new PdpAttribute(m.get("name"), m.get("value"))).collect(Collectors.toList());
        this.loas = ((List<Map<String, Object>>) data.getOrDefault("loas", new ArrayList<>()))
                .stream().map(m -> new LoA(
                        (String) m.get("level"),
                        (boolean) m.getOrDefault("allAttributesMustMatch", false),
                        (boolean) m.getOrDefault("negateCidrNotation", false),
                        ((List<Map<String, String>>) m.getOrDefault("attributes", new ArrayList<>())).stream()
                                .map(attr -> new PdpAttribute(attr.get("name"), attr.get("value"))).collect(Collectors.toList()),
                        ((List<Map<String, Object>>) m.getOrDefault("cidrNotations", new ArrayList<>())).stream()
                                .map(cidr -> new CidrNotation((String) cidr.get("ipAddress"), (Integer) cidr.get("prefix"))).collect(Collectors.toList())
                )).collect(Collectors.toList());
        this.denyRule = (boolean) data.getOrDefault("denyRule", false);
        this.allAttributesMustMatch = (boolean) data.getOrDefault("allAttributesMustMatch", false);
        this.created = (Date) data.get("created");
        this.userDisplayName = (String) data.get("userDisplayName");
        this.authenticatingAuthorityName = (String) data.get("authenticatingAuthorityName");
        this.denyAdvice = (String) data.get("denyAdvice");
        this.denyAdviceNl = (String) data.get("denyAdvice");
        this.active=(boolean) data.getOrDefault("allAttributesMustMatch", false);
        this.type=(String) data.get("type");
    }
}
