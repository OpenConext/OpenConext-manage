package manage.policies;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import manage.model.MetaData;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class PdpPolicyDefinition {

    private Long id;
    private String name;
    private String description;

    private List<String> serviceProviderIds = new ArrayList<>();
    private List<String> serviceProviderNames = new ArrayList<>();
    private List<String> serviceProviderNamesNl = new ArrayList<>();

    private boolean serviceProviderInvalidOrMissing;

    private List<String> identityProviderIds = new ArrayList<>();
    private List<String> identityProviderNames = new ArrayList<>();
    private List<String> identityProviderNamesNl = new ArrayList<>();

    private String clientId;

    private List<PdpAttribute> attributes = new ArrayList<>();

    private List<LoA> loas = new ArrayList<>();

    private String denyAdvice;

    private boolean denyRule;

    private boolean allAttributesMustMatch;

    private Date created;

    private String userDisplayName;

    private String authenticatingAuthorityName;

    private String denyAdviceNl;

    private int revisionNbr;

    private boolean isActivatedSr;

    private boolean active;

    private boolean actionsAllowed;

    private String type;

    private Long parentId;

    public PdpPolicyDefinition(MetaData metaData) {

    }
}
