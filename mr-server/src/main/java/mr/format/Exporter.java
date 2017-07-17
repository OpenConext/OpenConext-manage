package mr.format;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import mr.model.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/*
 * Thread-safe
 */
@SuppressWarnings("unchecked")
public class Exporter {

    private final MustacheFactory mf = new DefaultMustacheFactory();

    private Clock clock;

    public final static List<String> excludedDataFields = Arrays.asList("id", "eid", "revisionid", "user", "created", "ip", "revisionnote", "notes");

    public Exporter(Clock clock) {
        this.clock = clock;
    }

    public String exportToXml(MetaData metaData) {
        Mustache mustache = mf.compile(String.format("export/%s.xml", metaData.getType()));
        StringWriter writer = new StringWriter();
        try {
            Map data = Map.class.cast(metaData.getData());

            this.addOrganizationName(data);
            this.addValidUntil(data);

            mustache.execute(writer, data).flush();
            String xml = writer.toString();

            return xml;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> exportToMap(MetaData metaData, boolean nested) {
        Map<String, Object> data = Map.class.cast(metaData.getData());
        final Map<String, Object> result = new TreeMap<>();
        if (nested) {
            data.forEach((key, value) -> this.addKeyValue(key, value, result));
        } else {
            result.putAll(data);
            result.put("metaDataFields",new TreeMap(Map.class.cast(result.get("metaDataFields"))));
        }
        this.excludedDataFields.forEach(result::remove);
        return result;
    }

    private void addKeyValue(String key, Object value, Map<String, Object> result) {
        if (value instanceof Map) {
            Map<String, Object> map = new TreeMap();
            result.put(key, map);
            Map.class.cast(value).forEach((mapKey, mapValue) -> this.addKeyValue(String.class.cast(mapKey), mapValue, map));
            return;
        }
        if (value instanceof List) {
            if ("allowedEntities".equals(key) || "disableConsent".equals(key)) {
                List<Map<String, String>> values = List.class.cast(value);
                List<String> list = new ArrayList<>();
                result.put(key, list);
                values.forEach(mapValue -> list.add(mapValue.get("name")));
                return;
            } else {
                result.put(key, value);
                return;
            }
        }
        List<String> parts = Arrays.asList(key.split(":"));
        if (parts.size() == 1) {
            result.put(key, value);
        } else {
            Map<String, Object> reference = result;
            List<String> subParts = parts.subList(0, parts.size() - 1);
            //Lambda's requires final references
            for (String subKey : subParts) {
                if (!reference.containsKey(subKey)) {
                    reference.put(subKey, new TreeMap<String, Object>());
                }
                reference = Map.class.cast(reference.get(subKey));
            }
            reference.put(parts.get(parts.size()-1), value);
        }
    }

    private void addValidUntil(Map data) {
        ZonedDateTime now = ZonedDateTime.of(LocalDateTime.now(clock), ZoneId.systemDefault());
        String validUntil = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now.plusYears(1L));
        data.put("validUntil", validUntil);
    }

    private void addOrganizationName(Map data) {
        Map metaDataFields = Map.class.cast(data.get("metaDataFields"));
        String name = String.class.cast(metaDataFields.computeIfAbsent("OrganizationName:en", key -> metaDataFields.get("OrganizationName:nl")));
        if (StringUtils.hasText(name)) {
            metaDataFields.put("OrganizationName", name);
        }
        String displayName = String.class.cast(metaDataFields.computeIfAbsent("OrganizationDisplayName:en", key -> metaDataFields.get("OrganizationDisplayName:nl")));
        if (StringUtils.hasText(displayName)) {
            metaDataFields.put("OrganizationDisplayName", displayName);
        }
        String url = String.class.cast(metaDataFields.computeIfAbsent("OrganizationURL:en", key -> metaDataFields.get("OrganizationURL:nl")));
        if (StringUtils.hasText(url)) {
            metaDataFields.put("OrganizationURL", url);
        }
    }

}
