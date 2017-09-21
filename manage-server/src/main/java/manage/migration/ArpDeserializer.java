package manage.migration;

import com.github.ooxi.phparser.SerializedPhpParser;
import com.github.ooxi.phparser.SerializedPhpParserException;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.hasText;

public class ArpDeserializer {

    private static final Map<String, Object> NO_ARP = new HashMap<>();

    static {
        NO_ARP.put("enabled", false);
        NO_ARP.put("attributes", new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> parseArpAttributes(String input) {
        if (StringUtils.isEmpty(input) || input.equalsIgnoreCase("N;")) {
            return NO_ARP;
        }
        //SerializedPhpParser is not thread-safe
        SerializedPhpParser serializedPhpParser = new SerializedPhpParser(input);
        Object parse;
        try {
            parse = serializedPhpParser.parse();
        } catch (SerializedPhpParserException e) {
            throw new RuntimeException(e);
        }
        Map<String, Map<Long, Object>> arp = Map.class.cast(parse);

        Map<String, Object> result = new HashMap<>();
        result.put("enabled", true);

        Map<String, List<Map<String, String>>> attributes = new HashMap<>();
        result.put("attributes", attributes);

        arp.forEach((key, value) -> {
            attributes.put(key, value.values().stream().map(this::arpValue).collect(toList()));
        });
        return result;
    }

    private Map<String, String> arpValue(Object o) {
        //Supports old & new style
        if (o instanceof String) {
            String value = String.class.cast(o);
            Map<String, String> result = new HashMap<>();
            result.put("source", "idp");
            result.put("value", hasText(value) ? value : "*" );
            return result;
        } else {
            Map<String, String> map = (Map<String, String>) o;
            map.compute("value", (key, oldValue) -> hasText(oldValue) ? oldValue : "*");
            map.compute("source", (key, oldValue) -> hasText(oldValue) ? oldValue : "idp");
            return map;
        }
    }

}
