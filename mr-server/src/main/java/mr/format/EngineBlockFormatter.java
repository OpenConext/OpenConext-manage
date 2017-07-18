package mr.format;

import mr.model.MetaData;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.toList;

public class EngineBlockFormatter {

    public Map<String, Object> parseServiceProvider(MetaData metaDataContainer) {
        Map<String, Object> data = metaDataContainer.getData();

        Map<String, Object> serviceProvider = new TreeMap<>();
        serviceProvider.put("type", "saml20-sp");

        addCommonProviderAttributes(data, serviceProvider);

        Map<String, Object> metaData = (Map<String, Object>) serviceProvider.get("metadata");

        Map<String, String> metaDataFields = (Map<String, String>) data.get("metaDataFields");
        nest(metaDataFields, "", metaData);

        return serviceProvider;

    }

    public Map<String, Object> parseIdentityProvider(MetaData metaDataContainer) {
        Map<String, Object> data = metaDataContainer.getData();

        Map<String, Object> identityProvider = new TreeMap<>();
        identityProvider.put("type", "saml20-sp");
        identityProvider.put("disable_consent_connections", this.convertList(data.get("disableConsent")));

        addCommonProviderAttributes(data, identityProvider);

        Map<String, Object> metaData = (Map<String, Object>) identityProvider.get("metadata");

        Map<String, String> metaDataFields = (Map<String, String>) data.get("metaDataFields");
        nest(metaDataFields, "", metaData);

        return identityProvider;
    }

    private void addCommonProviderAttributes(Map<String, Object> data, Map<String, Object> provider) {
        provider.put("name", data.get("entityid"));
        provider.put("state", data.get("state"));
        provider.put("is_active", data.get("active"));
        String manipulation = String.class.cast(data.get("manipulation"));
        if (StringUtils.hasText(manipulation)) {
            provider.put("manipulation_code", manipulation);
        }
        provider.put("allowed_connections", this.convertList(data.get("allowedEntities")));

        Map<String, Object> metaData = new TreeMap<>();

        provider.put("metadata", metaData);

        Map<String, String> metaDataFields = (Map<String, String>) data.get("metaDataFields");
        nest(metaDataFields, "name:nl", metaData);
        nest(metaDataFields, "name:en", metaData);
        nest(metaDataFields, "displayName:nl", metaData);
        nest(metaDataFields, "displayName:en", metaData);
        nest(metaDataFields, "description:nl", metaData);
        nest(metaDataFields, "description:en", metaData);
        nest(metaDataFields, "keywords:en", metaData);
        nest(metaDataFields, "logo:0:height", metaData);
        nest(metaDataFields, "logo:0:url", metaData);
        nest(metaDataFields, "logo:0:width", metaData);
        nest(metaDataFields, "OrganizationName:nl", metaData);
        nest(metaDataFields, "OrganizationName:en", metaData);
        nest(metaDataFields, "OrganizationDisplayName:nl", metaData);
        nest(metaDataFields, "OrganizationDisplayName:en", metaData);
        nest(metaDataFields, "OrganizationURL:nl", metaData);
        nest(metaDataFields, "OrganizationURL:en", metaData);
        nest(metaDataFields, "keywords:nl", metaData);
        nest(metaDataFields, "coin:publish_in_edugain", metaData);
        nest(metaDataFields, "certData", metaData);
        nest(metaDataFields, "certData2", metaData);
        nest(metaDataFields, "certData3", metaData);
        nest(metaDataFields, "SingleLogoutService_Location", metaData);
        nest(metaDataFields, "SingleLogoutService_Binding", metaData);
        nest(metaDataFields, "NameIDFormat", metaData);
        IntStream.range(0, 10).forEach(i -> nest(metaDataFields, "NameIDFormat:" + i, metaData, true));
        singleLogOutService(metaDataFields, metaData);
        nest(metaDataFields, "", metaData);
        nest(metaDataFields, "", metaData);
        nest(metaDataFields, "", metaData);

    }


    private void singleLogOutService(Map<String, String> source, Map<String, Object> target) {
        String location = source.get("SingleLogoutService_Location");
        String binding = source.get("SingleLogoutService_Binding");
        if (!StringUtils.hasText(location) && !StringUtils.hasText(binding)) {
            return;
        }
        List<Map<String, String>> subList = new ArrayList<>();
        Map<String, String> map = new TreeMap<>();
        if (StringUtils.hasText(location)) {
            map.put("Location", location);
        }
        if (StringUtils.hasText(binding)) {
            map.put("Binding", binding);
        }
        subList.add(map);
        target.put("SingleLogoutService", subList);
    }

    private void nest(Map<String, String> source, String key, Map<String, Object> target) {
        nest(source, key, target, false);
    }

    private void nest(Map<String, String> source, String key, Map<String, Object> target, boolean isArray) {
        String value = source.get(key);
        if (!StringUtils.hasText(value)) {
            return;
        }
        List<String> parts = Arrays.asList(key.split(":"));
        if (parts.size() == 1) {
            target.put(key, value);
        } else if (parts.size() == 2) {
            if (isArray) {
                List<String> subList = (List<String>) target.getOrDefault(parts.get(0), new ArrayList<String>());
                subList.add(value);
                target.put(parts.get(0), subList);

            } else {
                Map<String, String> subMap = (Map<String, String>) target.getOrDefault(parts.get(0), new TreeMap<String, String>());
                subMap.put(parts.get(1), value);
                target.put(parts.get(0), subMap);
            }
        } else if (parts.size() == 3) {
            List<Map<String, String>> subList = (List<Map<String, String>>) target.getOrDefault(parts.get(0), new ArrayList<Map<String, String>>());
            subList.add(Collections.singletonMap(parts.get(2), value));
            target.put(parts.get(0), subList);
        }
    }

    private List<String> convertList(Object o) {
        List<Map<String, String>> entities = (List<Map<String, String>>) o;
        return entities.stream().map(entry -> entry.get("name")).collect(toList());
    }


}
