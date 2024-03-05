package manage.policies;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import manage.model.MetaData;
import manage.model.Revision;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;


@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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

    private Instant created;

    private String userDisplayName;

    private String authenticatingAuthorityName;

    private String denyAdvice;

    private String denyAdviceNl;

    private boolean isActivatedSr;

    private boolean active;

    private boolean actionsAllowed;

    private String type;

    private int revisionNbr;

    public PdpPolicyDefinition(MetaData metaData) {
        //this code ensures backward-compatibility for Dashboard if it switches from PdP API to Manage API
        this.id = metaData.getId();
        Map<String, Object> data = metaData.getData();
        this.name = (String) data.get("name");
        this.description = (String) data.get("description");
        this.serviceProviderIds = convertProviders(data, "serviceProviderIds");
        this.identityProviderIds = convertProviders(data, "identityProviderIds");
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
        this.userDisplayName = (String) data.get("userDisplayName");
        this.authenticatingAuthorityName = (String) data.get("authenticatingAuthorityName");
        this.denyAdvice = (String) data.get("denyAdvice");
        this.denyAdviceNl = (String) data.get("denyAdvice");
        this.active=(boolean) data.getOrDefault("active", false);
        this.type=(String) data.get("type");
        Revision revision = metaData.getRevision();
        this.created = revision.getCreated();
        this.revisionNbr = revision.getNumber();
    }

    private List<String> convertProviders(Map<String, Object> data, String name) {
        return ((List<Map<String, String>>) data.get(name)).stream().map(m -> m.get("name")).collect(Collectors.toList());
    }

    public static void updateProviderStructure(Map<String, Object> data) {
        List.of("identityProviderIds", "serviceProviderIds")
                .forEach(reference -> {
                            List<String> names = (List<String>) data.getOrDefault(reference, new ArrayList<>());
                            //The map needs to be mutable hence the extra HashMap constructor
                            data.put(reference, names.stream().map(name -> new HashMap<>(Map.of("name", name))).collect(toList()));
                        }
                );
        data.put("metaDataFields", new HashMap<>());
        List.of("id", "created").forEach(name -> data.remove(name));
    }

}
