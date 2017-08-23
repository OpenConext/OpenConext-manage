package mr.migration;

import com.github.ooxi.phparser.SerializedPhpParser;
import com.github.ooxi.phparser.SerializedPhpParserException;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

public class ArpDeserializer {

    @SuppressWarnings("unchecked")
    public ArpAttributes parseArpAttributes(String input) {
        if (StringUtils.isEmpty(input) || input.equalsIgnoreCase("N;")) {
            return ArpAttributes.noArp();
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
        return new ArpAttributes(true, arp.entrySet().stream().map(this::parseEntry).reduce(new HashMap<>(), (acc, map) -> {
            acc.putAll(map);
            return acc;
        }));
    }

    private Map<String, List<ArpValue>> parseEntry(Map.Entry<String, Map<Long, Object>> entry) {
        Map<String, List<ArpValue>> result = new HashMap<>();

            result.put(
                entry.getKey(),
                entry.getValue().values().stream()
                    .map(value -> arpValue(value)).collect(toList()));
        return result;
    }

    private ArpValue arpValue(Object o) {
        //Supports old & new style
        if  (o instanceof String) {
            return new ArpValue(String.class.cast(o), "idp");
        } else {
            Map<String, String> map = (Map<String, String> ) o;
            String value = map.getOrDefault("value", "*");
            value = value.trim().length() == 0 ? "*" : value;
            String source = map.getOrDefault("source", "idp");
            return new ArpValue(value, source);
        }
    }

}
