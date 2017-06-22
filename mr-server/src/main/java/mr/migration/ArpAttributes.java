package mr.migration;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@AllArgsConstructor
public class ArpAttributes {

    private boolean enabled;
    private Map<String, List<ArpValue>> attributes;

    public static ArpAttributes noArp() {
        return new ArpAttributes(false, new HashMap<>());
    }
}
