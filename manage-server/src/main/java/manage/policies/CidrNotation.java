package manage.policies;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CidrNotation {

    private String ipAddress;
    private int prefix;
    private IPInfo ipInfo;

}
