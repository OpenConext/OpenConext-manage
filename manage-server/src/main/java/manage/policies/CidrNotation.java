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
    private Integer prefix;
    private IPInfo ipInfo;

    public CidrNotation(String ipAddress, Integer prefix) {
        this.ipAddress = ipAddress;
        this.prefix = prefix;
        this.ipInfo = IPAddressProvider.getIpInfo(ipAddress, prefix);
    }
}
