package manage.policies;

import lombok.SneakyThrows;

import java.net.Inet4Address;
import java.net.InetAddress;

public class IPAddressProvider {

    private IPAddressProvider() {
    }

    @SneakyThrows
    public static IPInfo getIpInfo(String ipAddress, Integer networkPrefix) {
        InetAddress address = InetAddress.getByName(ipAddress);
        boolean isIpv4 = address instanceof Inet4Address;
        if (networkPrefix == null) {
            networkPrefix = isIpv4 ? 24 : 64;
        }
        CIDRUtils cidrUtils = new CIDRUtils(ipAddress.concat("/").concat(networkPrefix.toString()));
        int byteSize = isIpv4 ? 32 : 128;
        double capacity = Math.pow(2, byteSize - networkPrefix);
        return new IPInfo(cidrUtils.getNetworkAddress(), cidrUtils.getBroadcastAddress(), capacity, isIpv4, networkPrefix);
    }

}
