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
        Map<String, Map<Long, String>> arp = Map.class.cast(parse);
        return new ArpAttributes(true, arp.entrySet().stream().map(this::parseEntry).reduce(new HashMap<>(), (acc, map) -> {
            acc.putAll(map);
            return acc;
        }));
    }

    private Map<String, List<ArpValue>> parseEntry(Map.Entry<String, Map<Long, String>> entry) {
        Map<String, List<ArpValue>> result = new HashMap<>();
        //TODO once we have the new SR database we can also extract the correct source
        result.put(entry.getKey(), entry.getValue().values().stream().map(value -> new ArpValue(value, "idp")).collect(toList()));
        return result;
    }

}
