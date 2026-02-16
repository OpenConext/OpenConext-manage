package manage.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import manage.conf.MetaDataAutoConfiguration;
import manage.format.MetaDataFeedParser;
import manage.format.SaveURLResource;
import manage.hook.TypeSafetyHook;
import manage.model.EntityType;
import manage.model.Import;
import manage.model.MetaData;
import org.apache.commons.io.IOUtils;
import org.everit.json.schema.ValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

@SuppressWarnings("unchecked")
@Service
public class ImporterService {

    public static final String META_DATA_FIELDS = "metaDataFields";
    public static final String ARP = "arp";

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    private MetaDataFeedParser metaDataFeedParser;

    private Environment environment;

    private TypeSafetyHook metaDataHook;

    private String autoRefreshUserAgent;

    public ImporterService(MetaDataAutoConfiguration metaDataAutoConfiguration, Environment environment,
                           @Value("${product.supported_languages}") String supportedLanguages,
                           @Value("${metadata_import.useragent:OpenConext-Manage}") String autoRefreshUserAgent) {

        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.environment = environment;
        this.metaDataHook = new TypeSafetyHook(metaDataAutoConfiguration);
        this.metaDataFeedParser = new MetaDataFeedParser(Stream.of(
                supportedLanguages.split(","))
                .map(String::trim)
                .collect(toList()));
        this.autoRefreshUserAgent = autoRefreshUserAgent;
    }

    public Map<String, Object> importXMLUrl(EntityType type, Import importRequest) {
        try {
            Resource resource = new SaveURLResource(new URI(importRequest.getUrl()).toURL(),
                    environment.acceptsProfiles(Profiles.of("dev")), autoRefreshUserAgent);
            Map<String, Object> result = importXML(resource, type, Optional
                    .ofNullable(importRequest.getEntityId()));
            if (result.isEmpty()) {
                return singletonMap("errors", singletonList("URL did not contain valid SAML metadata"));
            }
            result.put("metadataurl", importRequest.getUrl());
            return result;
        } catch (IOException | XMLStreamException | URISyntaxException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
        }
    }

    public Map<String, Object> importXML(Resource resource, EntityType entityType, Optional<String> entityId)
            throws IOException, XMLStreamException {
        return metaDataFeedParser.importXML(resource, entityType, entityId, metaDataAutoConfiguration);
    }

    public List<Map<String, Object>> importFeed(Resource resource) throws IOException,
            XMLStreamException {
        return metaDataFeedParser.importFeed(resource, metaDataAutoConfiguration);
    }

    public Map<String, Object> importJson(String type, Map<String, Object> json) throws JsonProcessingException {
        EntityType entityType = getType(type, json);
        try {
            return importJSON(entityType, json);
        } catch (ValidationException e) {
            Map<String, Object> result = new HashMap<>();
            result.put("errors", e.getAllMessages());
            result.put("type", entityType.getType());
            return result;
        }
    }

    public Map<String, Object> importJsonUrl(String type, Import importRequest) {
        try {
            Resource resource = new SaveURLResource(new URI(importRequest.getUrl()).toURL(),
                    environment.acceptsProfiles(Profiles.of("dev")), autoRefreshUserAgent);
            String json = IOUtils.toString(resource.getInputStream(), Charset.defaultCharset());
            Map<String, Object> map = objectMapper.readValue(json, Map.class);
            return importJson(type, map);
        } catch (IOException | URISyntaxException e) {
            return singletonMap("errors", singletonList(e.getClass().getName()));
        }
    }

    public Map<String, Object> importJSON(EntityType entityType, Map<String, Object> data)
            throws JsonProcessingException {
        data.entrySet().removeIf(entry -> entry.getValue() == null);

        Map<String, Object> json = new ConcurrentHashMap<>(data);
        Object metaDataFieldsMap = json.get(META_DATA_FIELDS);
        if (!(metaDataFieldsMap instanceof Map)) {
            metaDataAutoConfiguration.validate(json, entityType.getType());
            return Collections.EMPTY_MAP;
        }
        Map<String, Object> metaDataFields = new ConcurrentHashMap<>((Map<String, Object>) metaDataFieldsMap);
        json.put(META_DATA_FIELDS, metaDataFields);

        if (!entityType.equals(EntityType.IDP) && json.containsKey("disableConsent")) {
            json.remove("disableConsent");
        }
        if (!entityType.equals(EntityType.IDP) && json.containsKey("stepupEntities")) {
            json.remove("stepupEntities");
        }
        if (!entityType.equals(EntityType.IDP) && json.containsKey("mfaEntities")) {
            json.remove("mfaEntities");
        }
        if (!entityType.equals(EntityType.RP) && json.containsKey("allowedResourceServers")) {
            json.remove("allowedResourceServers");
        }

        if (metaDataFields.values().stream().anyMatch(value -> value instanceof Map)) {
            if (json.containsKey("allowedEntities")) {
                List<String> allowedEntities = (List<String>) json.get("allowedEntities");
                json.put("allowedEntities", allowedEntities.stream()
                        .map(name -> Collections.singletonMap("name", name)).collect(toList()));
            }
            if (json.containsKey("disableConsent")) {
                List<String> disableConsent = (List<String>) json.get("disableConsent");
                json.put("disableConsent", disableConsent.stream()
                        .map(name -> Collections.singletonMap("name", name)).collect(toList()));
            }

            if (json.containsKey("stepupEntities")) {
                List<String> stepupEntities = (List<String>) json.get("stepupEntities");
                json.put("stepupEntities", stepupEntities.stream()
                        .map(name -> Collections.singletonMap("name", name)).collect(toList()));
            }
            if (json.containsKey("mfaEntities")) {
                List<String> mfaEntities = (List<String>) json.get("mfaEntities");
                json.put("mfaEntities", mfaEntities.stream()
                        .map(name -> Collections.singletonMap("name", name)).collect(toList()));
            }
            //if the structure is nested then we need to flatten it
            Map<String, Object> flattened = new ConcurrentHashMap<>();
            metaDataFields.entrySet().forEach(entry -> {
                Object value = entry.getValue();
                if (value instanceof String) {
                    flattened.put(entry.getKey(), value);
                }
                if (value instanceof Map) {
                    String keyPrefix = entry.getKey();
                    flatten(keyPrefix, (Map<String, Object>) value, flattened);
                }
            });
            json.put(META_DATA_FIELDS, flattened);
        }
        ExporterService.excludedDataFields.forEach(json::remove);

        MetaData metaData = metaDataHook.preValidate(new MetaData(entityType.getType(), json));
        Map<String, Object> migratedData = metaData.getData();

        metaDataAutoConfiguration.validate(migratedData, entityType.getType());

        return new TreeMap<>(migratedData);
    }

    private void flatten(String keyPrefix, Map<String, Object> value, Map<String, Object> target) {
        value.forEach((key, entryValue) -> {
            if (entryValue instanceof String) {
                target.put(keyPrefix + ":" + key, entryValue);
            }
            if (entryValue instanceof Map) {
                flatten(keyPrefix + ":" + key, (Map<String, Object>) entryValue, target);
            }
        });
    }

    private EntityType getType(String type, Map<String, Object> json) {
        EntityType entityType = EntityType.IDP.getType().equals(type) ?
                EntityType.IDP : EntityType.SP.getType().equals(type) ? EntityType.SP : null;
        if (entityType == null) {
            Object jsonType = json.get("type");
            if (jsonType == null) {
                throw new IllegalArgumentException("Expected a 'type' attribute in the JSON with value 'saml20-idp' " +
                        "or 'saml20-sp'");
            }
            return EntityType.fromType((String) jsonType);
        }
        return entityType;
    }

}
