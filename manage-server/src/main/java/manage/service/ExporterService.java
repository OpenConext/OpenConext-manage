package manage.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import manage.model.MetaData;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("unchecked")
@Service
public class ExporterService {

    static final List<String> excludedDataFields =
            Arrays.asList("id", "eid", "revisionid", "user", "created", "ip", "revisionnote", "notes");
    private static final List<String> validMetadataExportTypes =
            Arrays.asList("saml20_idp", "saml20_sp", "single_tenant_template");
    private static final MustacheFactory MUSTACHE_FACTORY = new DefaultMustacheFactory();

    private static final List<String> excludeMetaDataOnlyKeys =
            Arrays.asList("allowedEntities", "arp", "disableConsent", "active", "manipulation");

    private final ResourceLoader resourceLoader;

    private final Clock clock;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("#{'${product.supported_languages}'.split(',')}")
    private List<String> languages;

    @Value("${metadata_export_path}")
    private String metadataExportPath;

    public ExporterService(ResourceLoader resourceLoader) {

        this.clock = Clock.systemDefaultZone();
        this.resourceLoader = resourceLoader;
    }

    public Map<String, Object> export(MetaData metaData) throws IOException {
        Map<String, Object> result = new HashMap<>();

        metaData.getData().entrySet().removeIf(entry -> entry.getValue() == null);

        Map<String, Object> nested = exportToMap(metaData, true);
        Map<String, Object> flat = exportToMap(metaData, false);

        ObjectWriter objectWriter = objectMapper.writerWithDefaultPrettyPrinter();

        result.put("json", objectWriter.writeValueAsString(nested));
        result.put("jsonFlat", objectWriter.writeValueAsString(flat));
        if (validMetadataExportTypes.contains(metaData.getType())) {
            result.put("xml", exportToXml(metaData));
        }
        Map<String, Object> metaDataOnlyNested = new TreeMap<>(nested);
        excludeMetaDataOnlyKeys.forEach(metaDataOnlyNested::remove);
        result.put("jsonMetaDataOnly", objectWriter.writeValueAsString(metaDataOnlyNested));

        Map<String, Object> metaDataOnlyFlat = new TreeMap<>(flat);
        excludeMetaDataOnlyKeys.forEach(metaDataOnlyFlat::remove);
        result.put("jsonMetaDataOnlyFlat", objectWriter.writeValueAsString(metaDataOnlyFlat));

        return result;
    }

    public String exportToXml(MetaData metaData) throws IOException {
        String type = metaData.getType();
        if (!validMetadataExportTypes.contains(type)) {
            throw new IllegalArgumentException(String.format("Not allowed metaData type %s. Allowed are %s",
                    type, validMetadataExportTypes));
        }
        String path = String.format("%s/%s.xml", metadataExportPath, type);
        Resource resource = resourceLoader.getResource(path);
        String template = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());

        Mustache mustache = MUSTACHE_FACTORY.compile(new StringReader(template), type);
        StringWriter writer = new StringWriter();
        try {
            Map data = Map.class.cast(metaData.getData());

            addOrganizationName(data);
            addValidUntil(data);
            addAttributeConsumingService(data);
            addUIInfoExtension(data);
            addUILogo(data);

            mustache.execute(writer, data).flush();
            String xml = writer.toString();

            return xml;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> nestMetaData(Map metaData, String type) {
        return doExportToMap(true, metaData, type);
    }

    public Map<String, Object> exportToMap(MetaData metaData, boolean nested) {
        Map<String, Object> data = metaData.getData();
        return doExportToMap(nested, data, metaData.getType().replaceAll("_", "-"));
    }

    private Map<String, Object> doExportToMap(boolean nested, Map<String, Object> data, String type) {
        final Map<String, Object> result = new TreeMap<>();
        if (nested) {
            data.forEach((key, value) -> addKeyValue(key, value, result));
        } else {
            result.putAll(data);
            result.put("metaDataFields", new TreeMap(Map.class.cast(result.get("metaDataFields"))));
        }
        excludedDataFields.forEach(result::remove);
        result.put("type", type);
        return result;
    }

    private void addKeyValue(String key, Object value, Map<String, Object> result) {
        if (value instanceof Map) {
            Map<String, Object> map = new TreeMap();
            result.put(key, map);
            Map.class.cast(value).forEach((mapKey, mapValue) -> addKeyValue(String.class.cast(mapKey), mapValue,
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
        ZonedDateTime zonedDateTime = ZonedDateTime.of(LocalDateTime.now(clock),
                TimeZone.getTimeZone("UTC").toZoneId());

        String validUntil = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(zonedDateTime.plusYears(1L));
        data.put("validUntil", validUntil);
    }

    private void addOrganizationName(Map data) {
        Map metaDataFields = Map.class.cast(data.get("metaDataFields"));
        String name = addLanguageFallbackValue(metaDataFields, "OrganizationName");
        String displayName = addLanguageFallbackValue(metaDataFields, "OrganizationDisplayName");
        String url = addLanguageFallbackValue(metaDataFields, "OrganizationName");
        metaDataFields.put("OrganizationInfo", StringUtils.hasText(name) || StringUtils.hasText(displayName) ||
                StringUtils.hasText(url));
    }

    private void addAttributeConsumingService(Map data) {
        boolean arpAttributes = false;
        Object possibleArp = data.get("arp");
        if (possibleArp instanceof Map) {
            Map arp = Map.class.cast(possibleArp);
            if (Boolean.class.cast(arp.getOrDefault("enabled", false))) {
                Object attributesObject = arp.get("attributes");
                if (attributesObject != null) {
                    if (attributesObject instanceof Map) {
                        Map attributes = Map.class.cast(attributesObject);
                        arpAttributes = !attributes.isEmpty();
                        if (arpAttributes) {
                            data.put("requestedAttributes", attributes.keySet());
                        }
                    } else if (attributesObject instanceof List) {
                        List attributes = List.class.cast(attributesObject);
                        arpAttributes = !attributes.isEmpty();
                        if (arpAttributes) {
                            data.put("requestedAttributes", new HashSet<>(attributes));
                        }

                    }
                }
            }
        }
        data.put("AttributeConsumingService", arpAttributes);
    }

    private void addUIInfoExtension(Map data) {
        Map metaDataFields = Map.class.cast(data.get("metaDataFields"));
        String name = addLanguageFallbackValue(metaDataFields, "name");
        String description = addLanguageFallbackValue(metaDataFields, "description");
        data.put("UIInfoExtension", StringUtils.hasText(name) || StringUtils.hasText(description));
    }

    private String addLanguageFallbackValue(Map metaDataFields, String attribute) {
        AtomicReference<String> reference = new AtomicReference<>();
        languages.forEach(lang -> {
            if (StringUtils.isEmpty(reference.get())) {
                reference.set((String) metaDataFields.get(attribute + ":" + lang));
            }
        });
        if (StringUtils.hasText(reference.get())) {
            languages.forEach(lang ->
                    metaDataFields.computeIfAbsent(attribute + ":" + lang, key -> reference.get()));
        }
        return reference.get();
    }

    private void addUILogo(Map data) {
        Map metaDataFields = Map.class.cast(data.get("metaDataFields"));
        Object height = metaDataFields.get("logo:0:height");
        Object width = metaDataFields.get("logo:0:width");
        String url = (String) metaDataFields.get("logo:0:url");
        data.put("Logo", height != null && width != null && StringUtils.hasText(url));
    }

}