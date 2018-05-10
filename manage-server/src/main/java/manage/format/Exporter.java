package manage.format;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import manage.model.MetaData;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/*
 * Thread-safe
 */
@SuppressWarnings("unchecked")
public class Exporter {

    final static List<String> excludedDataFields = Arrays.asList("id", "eid", "revisionid", "user", "created", "ip",
        "revisionnote", "notes");
    private final static MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();
    private final ResourceLoader resourceLoader;
    private final String metadataExportPath;
    private final Clock clock;

    public Exporter(Clock clock, ResourceLoader resourceLoader, String metadataExportPath) {
        this.clock = clock;
        this.resourceLoader = resourceLoader;
        this.metadataExportPath = metadataExportPath;
    }

    public String exportToXml(MetaData metaData) throws IOException {
        String path = String.format("%s/%s.xml", metadataExportPath, metaData.getType());
        Resource resource = resourceLoader.getResource(path);
        String template = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());

        Mustache mustache = MUSTACHE_FACTORY.compile(new StringReader(template), metaData.getType());
        StringWriter writer = new StringWriter();
        try {
            Map data = Map.class.cast(metaData.getData());

            this.addOrganizationName(data);
            this.addValidUntil(data);
            this.addAttributeConsumingService(data);
            this.addUIInfoExtension(data);
            this.addUILogo(data);

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
            result.put("metaDataFields", new TreeMap(Map.class.cast(result.get("metaDataFields"))));
        }
        this.excludedDataFields.forEach(result::remove);
        result.put("type", metaData.getType().replaceAll("_", "-"));
        return result;
    }

    private void addKeyValue(String key, Object value, Map<String, Object> result) {
        if (value instanceof Map) {
            Map<String, Object> map = new TreeMap();
            result.put(key, map);
            Map.class.cast(value).forEach((mapKey, mapValue) -> this.addKeyValue(String.class.cast(mapKey), mapValue,
                map));
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
            reference.put(parts.get(parts.size() - 1), value);
        }
    }

    private void addValidUntil(Map data) {
        ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDateTime.now(clock), TimeZone.getTimeZone("UTC").toZoneId());
        String validUntil = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zonedDateTime.plusYears(1L));
        data.put("validUntil", validUntil);
    }

    private void addOrganizationName(Map data) {
        Map metaDataFields = Map.class.cast(data.get("metaDataFields"));
        String name = String.class.cast(metaDataFields.computeIfAbsent("OrganizationName:en", key -> metaDataFields
            .get("OrganizationName:nl")));
        String displayName = String.class.cast(metaDataFields.computeIfAbsent("OrganizationDisplayName:en", key ->
            metaDataFields.get("OrganizationDisplayName:nl")));
        String url = String.class.cast(metaDataFields.computeIfAbsent("OrganizationURL:en", key -> metaDataFields.get
            ("OrganizationURL:nl")));
        metaDataFields.put("OrganizationInfo", StringUtils.hasText(name) || StringUtils.hasText(displayName) ||
            StringUtils.hasText(url));
    }

    private void addAttributeConsumingService(Map data) {
        boolean arpAttributes = false;
        Map arp = Map.class.cast(data.getOrDefault("arp", new HashMap<>()));
        if (Boolean.class.cast(arp.getOrDefault("enabled", false))) {
            Map attributes = Map.class.cast(arp.getOrDefault("attributes", new HashMap<>()));
            arpAttributes = !attributes.isEmpty();
            if (arpAttributes) {
                data.put("requestedAttributes", attributes.keySet());
            }
        }
        data.put("AttributeConsumingService", arpAttributes);
    }

    private void addUIInfoExtension(Map data) {
        Map metaDataFields = Map.class.cast(data.get("metaDataFields"));
        String name = String.class.cast(metaDataFields.computeIfAbsent("name:en", key -> metaDataFields.get
            ("name:nl")));
        String description = String.class.cast(metaDataFields.computeIfAbsent("description:en", key -> metaDataFields
            .get("description:nl")));

        data.put("UIInfoExtension", StringUtils.hasText(name) || StringUtils.hasText(description));
    }

    private void addUILogo(Map data) {
        Map metaDataFields = Map.class.cast(data.get("metaDataFields"));
        String height = (String) metaDataFields.get("logo:0:height");
        String width = (String) metaDataFields.get("logo:0:width");
        String url = (String) metaDataFields.get("logo:0:url");
        data.put("Logo", StringUtils.hasText(height) && StringUtils.hasText(width) && StringUtils.hasText(url));
    }


}
