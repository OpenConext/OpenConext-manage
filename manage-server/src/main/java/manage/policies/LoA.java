package manage.policies;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.CollectionUtils;

import javax.validation.Valid;
import java.util.*;

import static java.util.stream.Collectors.groupingBy;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoA {

    private String level;

    private boolean allAttributesMustMatch;

    private boolean negateCidrNotation;

    @Valid
    private List<PdpAttribute> attributes = new ArrayList<>();

    @Valid
    private List<CidrNotation> cidrNotations = new ArrayList<>();

}
