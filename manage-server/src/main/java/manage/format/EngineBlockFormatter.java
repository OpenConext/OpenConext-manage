package manage.format;

import manage.model.EntityType;
import manage.model.MetaData;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.IntStream;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.springframework.util.StringUtils.hasText;

/**
 * Mimics the parsing of metadata from the
 * https://github.com/OpenConext/OpenConext-engineblock-metadata/blob/master/src/Entity/Assembler/JanusPushMetadataAssembler.php
 */
@SuppressWarnings("unchecked")
public class EngineBlockFormatter {

    private static final Map<String, Optional> commonAttributes = new TreeMap<>();
    private static final Map<String, Optional> spAttributes = new TreeMap<>();
    private static final Map<String, Optional> idpAttributes = new TreeMap<>();

    private static final int BEGIN_INDEX = "metadata:".length();

    static {
        commonAttributes.put("entityid", of("name"));
        commonAttributes.put("metadata:name:nl", empty());
        commonAttributes.put("metadata:name:en", empty());
        commonAttributes.put("metadata:displayName:en", empty());
        commonAttributes.put("metadata:displayName:nl", empty());
        commonAttributes.put("metadata:description:en", empty());
        commonAttributes.put("metadata:description:nl", empty());
        //logo is handled in separate method
        commonAttributes.put("metadata:OrganizationName:nl", empty());
        commonAttributes.put("metadata:OrganizationName:en", empty());
        commonAttributes.put("metadata:OrganizationDisplayName:nl", empty());
        commonAttributes.put("metadata:OrganizationDisplayName:en", empty());
        commonAttributes.put("metadata:OrganizationURL:nl", empty());
        commonAttributes.put("metadata:OrganizationURL:en", empty());

        commonAttributes.put("metadata:keywords:en", empty());
        commonAttributes.put("metadata:keywords:nl", empty());
        commonAttributes.put("metadata:url:en", empty());
        commonAttributes.put("metadata:url:nl", empty());

        commonAttributes.put("metadata:certData", empty());
        commonAttributes.put("metadata:certData2", empty());
        commonAttributes.put("metadata:certData3", empty());

        commonAttributes.put("state", empty());
        //contact persons are handled in separate method
        commonAttributes.put("metadata:NameIDFormat", empty());
        //single log outs are handled in separate method
        commonAttributes.put("manipulation", of("manipulation_code"));
    }

    public Map<String, Object> parseServiceProvider(MetaData metaDataContainer) {
        Map<String, Object> source = metaDataContainer.getData();

        Map<String, Object> serviceProvider = new TreeMap<>();
        serviceProvider.put("type", EntityType.SP.getJanusDbValue());

        addCommonProviderAttributes(source, serviceProvider);
        addNameIDFormats(source, serviceProvider);
        addAttributeReleasePolicy(source, serviceProvider);
        addAssertionConsumerService(source, serviceProvider);
        addPrivacyStatementURL(source, serviceProvider);

        spAttributes.forEach((key, value) -> this.addToResult(source, serviceProvider, key, value));

        removeEmptyValues(serviceProvider);
        return serviceProvider;

    }

    public Map<String, Object> parseIdentityProvider(MetaData metaDataContainer) {
        Map<String, Object> source = metaDataContainer.getData();

        Map<String, Object> identityProvider = new TreeMap<>();
        identityProvider.put("type", EntityType.IDP.getJanusDbValue());

        List<Map<String, String>> disableConsent = (List<Map<String, String>>) source.get("disableConsent");
        identityProvider.put("disable_consent_connections", disableConsent == null ? new ArrayList<>() : disableConsent);

        List<Map<String, String>> stepupEntities = (List<Map<String, String>>) source.get("stepupEntities");
        identityProvider.put("stepup_connections", stepupEntities == null ? new ArrayList<>() : stepupEntities);

        List<Map<String, String>> mfaEntities = (List<Map<String, String>>) source.get("mfaEntities");
        identityProvider.put("mfa_entities", mfaEntities == null ? new ArrayList<>() : mfaEntities);

        addCommonProviderAttributes(source, identityProvider);
        addSingleSignOnService(source, identityProvider);

        idpAttributes.forEach((key, value) -> this.addToResult(source, identityProvider, key, value));

        addShibMdScopes(source, identityProvider);

        removeEmptyValues(identityProvider);
        return identityProvider;
    }

    public Map<String, Object> parseOidcClient(MetaData metaDataContainer) {
        Map<String, Object> result = parseServiceProvider(metaDataContainer);
        Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", s -> new TreeMap<String, Object>());

        ArrayList<Object> assertionConsumerServiceContainer = new ArrayList<>();
        Map<String, String> assertionConsumerService = new TreeMap<>();
        //OpenIDIDConnect Relaying Parties entities do not have an ACS location, but we need to be backward compatible for EB
        assertionConsumerService.put("Binding", "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST");
        assertionConsumerService.put("Location", "https://trusted.proxy.acs.location.rules");
        assertionConsumerService.put("Index", "1");

        assertionConsumerServiceContainer.add(assertionConsumerService);
        metadata.put("AssertionConsumerService", assertionConsumerServiceContainer);
        return result;
    }

    private void addCommonProviderAttributes(Map<String, Object> source, Map<String, Object> result) {
        commonAttributes.forEach((key, value) -> this.addToResult(source, result, key, value));

        addCoinMetadataAttributesToResult(source, result);
        addLogo(source, result);
        addContactPersons(source, result);
        addSingleLogOutService(source, result);
        addRedirectSign(source, result);

        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) source.get("allowedEntities");
        result.put("allowed_connections", allowedEntities == null ? new ArrayList<>() : allowedEntities);
        result.put("allow_all_entities", source.getOrDefault("allowedall", false));
    }

    private void addLogo(Map<String, Object> source, Map<String, Object> result) {
        result = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");

        Object height = metaDataFields.get("logo:0:height");
        String url = (String) metaDataFields.get("logo:0:url");
        Object width = metaDataFields.get("logo:0:width");

        if (height != null || hasText(url) || width != null) {
            ArrayList<Object> logoContainer = new ArrayList<>();
            Map<String, String> logo = new HashMap<>();
            putIfHasText("height", height, logo);
            putIfHasText("url", url, logo);
            putIfHasText("width", width, logo);
            logoContainer.add(logo);
            result.put("logo", logoContainer);
        }
    }

    private void removeEmptyValues(Map<String, Object> result) {
        result.entrySet().removeIf(entry -> {
            if (entry.getValue() instanceof Map && !entry.getKey().equals("arp_attributes")) {
                Map<String, Object> map = (Map<String, Object>) entry.getValue();
                removeEmptyValues(map);
                return map.isEmpty();
            }
            return false;
        });
    }

    private void addContactPersons(Map<String, Object> source, Map<String, Object> result) {
        final Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new
                TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");
        IntStream.range(0, 4).forEach(i -> {
            String contactType = (String) metaDataFields.get("contacts:" + i + ":contactType");
            String emailAddress = (String) metaDataFields.get("contacts:" + i + ":emailAddress");
            String telephoneNumber = (String) metaDataFields.get("contacts:" + i + ":telephoneNumber");
            String givenName = (String) metaDataFields.get("contacts:" + i + ":givenName");
            String surName = (String) metaDataFields.get("contacts:" + i + ":surName");

            if (hasText(contactType) || hasText(emailAddress) || hasText(telephoneNumber) || hasText(givenName) || hasText(surName)) {
                ArrayList<Object> contactsContainer = (ArrayList<Object>) metadata.computeIfAbsent(
                        "contacts", key -> new ArrayList<>());
                Map<String, String> contact = new HashMap<>();
                putIfHasText("contactType", contactType, contact);
                putIfHasText("emailAddress", emailAddress, contact);
                putIfHasText("telephoneNumber", telephoneNumber, contact);
                putIfHasText("givenName", givenName, contact);
                putIfHasText("surName", surName, contact);
                contactsContainer.add(contact);
            }

        });
    }

    private void putIfHasText(String key, Object value, Map<String, String> result) {
        String sValue = parseValueToString(value);
        if (hasText(sValue)) {
            result.put(key, sValue);
        }
    }

    private String parseValueToString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Boolean) {
            return Boolean.class.cast(value) ? "1" : "0";
        }
        if (value instanceof Integer) {
            return Integer.class.cast(value).toString();
        }
        return value.toString();
    }

    private void addSingleLogOutService(Map<String, Object> source, Map<String, Object> result) {
        result = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");

        String location = (String) metaDataFields.get("SingleLogoutService_Location");
        String binding = (String) metaDataFields.get("SingleLogoutService_Binding");
        if (!hasText(location) && !hasText(binding)) {
            return;
        }
        List<Map<String, String>> subList = new ArrayList<>();
        Map<String, String> map = new TreeMap<>();
        putIfHasText("Location", location, map);
        putIfHasText("Binding", binding, map);
        subList.add(map);
        result.put("SingleLogoutService", subList);
    }

    private void addRedirectSign(Map<String, Object> source, Map<String, Object> result) {
        result = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");

        String redirectSign = parseValueToString(metaDataFields.get("redirect.sign"));
        if (hasText(redirectSign)) {
            Map<String, Boolean> redirect = new HashMap<>();
            redirect.put("sign", redirectSign.equalsIgnoreCase("1"));
            result.put("redirect", redirect);
        }
    }

    private void addNameIDFormats(Map<String, Object> source, Map<String, Object> result) {
        final Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new
                TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");

        IntStream.range(0, 3).forEach(i -> {
            String nameIdFormat = (String) metaDataFields.get("NameIDFormats:" + i);
            if (hasText(nameIdFormat)) {
                Set<String> nameIDFormats = (Set<String>) metadata.computeIfAbsent(
                        "NameIDFormats", key -> new HashSet<>());
                nameIDFormats.add(nameIdFormat);
            }
        });
    }

    private void addPrivacyStatementURL(Map<String, Object> source, Map<String, Object> result) {
        final Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new
                TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");
        List.of("en", "nl").forEach(lang -> {
            String privacyStatementURL = (String) metaDataFields.get("mdui:PrivacyStatementURL:" + lang);
            if (hasText(privacyStatementURL)) {
                Map<String, Object> privacyStatementURLs = (Map<String, Object>) metadata.computeIfAbsent("PrivacyStatementURL",
                        key -> new TreeMap<>());
                privacyStatementURLs.put(lang, privacyStatementURL);
            }
        });
    }

    private void addAttributeReleasePolicy(Map<String, Object> source, Map<String, Object> result) {
        Object possibleArp = source.get("arp");

        if (possibleArp == null || possibleArp instanceof List) {
            Map<String, List<Map<String, String>>> arpResult = new HashMap<>();
            result.put("arp_attributes", arpResult);
            return;
        }
        Map<String, Object> arp = (Map<String, Object>) possibleArp;
        Object enabled = arp.get("enabled");
        if (enabled != null && (Boolean) enabled) {
            Object possibleAttributes = arp.get("attributes");
            if (possibleAttributes instanceof List) {
                result.put("arp_attributes", possibleAttributes);
            } else if (possibleAttributes instanceof Map) {
                Map<String, List<Map<String, Object>>> attributes = (Map<String, List<Map<String, Object>>>) possibleAttributes;
                //bugfix for EB not having the knowledge that 'idp' source is special
                Collection<List<Map<String, Object>>> values = attributes.values();
                values.forEach(arpValues -> arpValues.forEach(map -> map.entrySet()
                        .removeIf(entry -> ("source".equals(entry.getKey()) && "idp".equals(entry.getValue()))
                        || (entry.getValue() instanceof String && !StringUtils.hasText((String) entry.getValue())))));
                result.put("arp_attributes", attributes);
            }
        }
    }

    private void addSingleSignOnService(Map<String, Object> source, Map<String, Object> result) {
        final Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new
                TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");
        IntStream.range(0, 10).forEach(i -> {
            String binding = (String) metaDataFields.get("SingleSignOnService:" + i + ":Binding");
            String location = (String) metaDataFields.get("SingleSignOnService:" + i + ":Location");

            if (hasText(binding) || hasText(location)) {
                ArrayList<Object> singleSignOnServiceContainer = (ArrayList<Object>) metadata.computeIfAbsent(
                        "SingleSignOnService", key -> new ArrayList<>());
                Map<String, String> singleSignOnService = new HashMap<>();
                putIfHasText("Binding", binding, singleSignOnService);
                putIfHasText("Location", location, singleSignOnService);
                singleSignOnServiceContainer.add(singleSignOnService);
            }

        });
    }

    private void addShibMdScopes(Map<String, Object> source, Map<String, Object> result) {
        final Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new
                TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");
        IntStream.range(0, 30).forEach(i -> {
            String allowed = parseValueToString(metaDataFields.get("shibmd:scope:" + i + ":allowed"));
            String regexp = parseValueToString(metaDataFields.get("shibmd:scope:" + i + ":regexp"));

            if (hasText(allowed) || hasText(regexp)) {
                Map<String, List<Object>> shibmdContainer = (Map<String, List<Object>>) metadata.computeIfAbsent(
                        "shibmd", key -> new HashMap<>());
                List<Object> scopeContainer = shibmdContainer.computeIfAbsent("scope", key -> new ArrayList<>());
                Map<String, Object> scope = new HashMap<>();
                if (hasText(allowed)) {
                    scope.put("allowed", allowed);
                }
                if (hasText("regexp")) {
                    scope.put("regexp", regexp);
                }
                scopeContainer.add(scope);
            }
        });
    }

    private void addAssertionConsumerService(Map<String, Object> source, Map<String, Object> result) {
        final Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new
                TreeMap<>());
        Map<String, Object> metaDataFields = (Map<String, Object>) source.get("metaDataFields");
        IntStream.range(0, 30).forEach(i -> {
            String binding = (String) metaDataFields.get("AssertionConsumerService:" + i + ":Binding");
            String location = (String) metaDataFields.get("AssertionConsumerService:" + i + ":Location");
            String index = parseValueToString(metaDataFields.get("AssertionConsumerService:" + i + ":index"));

            if (hasText(binding) || hasText(location)) {
                ArrayList<Object> assertionConsumerServiceContainer = (ArrayList<Object>) metadata.computeIfAbsent(
                        "AssertionConsumerService", key -> new ArrayList<>());
                Map<String, String> assertionConsumerService = new HashMap<>();
                putIfHasText("Binding", binding, assertionConsumerService);
                putIfHasText("Location", location, assertionConsumerService);
                putIfHasText("Index", index, assertionConsumerService);

                assertionConsumerServiceContainer.add(assertionConsumerService);
            }

        });
    }

    protected void addCoinMetadataAttributesToResult(Map<String, Object> source,
                                                     Map<String, Object> result) {
        Map<String, Object> metadata = (Map<String, Object>) source.get("metaDataFields");
        metadata.keySet().stream().filter(key -> key.startsWith("coin:"))
                .forEach(key -> {
                    addToResult(source, result, "metadata:" + key, empty());
                });
    }

    protected void addToResult(Map<String, Object> source,
                               Map<String, Object> result,
                               String compoundName,
                               Optional<String> convertTo) {
        List<String> parts = Arrays.asList(compoundName.split(":"));
        if (parts.size() == 1) {
            Object o = source.get(compoundName);
            if (o != null) {
                result.put(convertTo.orElse(compoundName), parseValueToString(o));
            }
            return;
        }
        Iterator<String> iterator = parts.iterator();
        Object value = null;
        while (iterator.hasNext()) {
            String part = iterator.next();
            if (part.equals("metadata")) {
                result = (Map<String, Object>) result.computeIfAbsent(part, key -> new TreeMap<String, Map<String,
                        Object>>());
                value = ((Map) source.get("metaDataFields")).get(compoundName.substring(BEGIN_INDEX));
            } else {
                if (iterator.hasNext()) {
                    result = (Map<String, Object>) result.computeIfAbsent(part, key -> new TreeMap<String,
                            Map<String, Object>>());
                } else if (value != null) {
                    result.put(convertTo.orElse(part), parseValueToString(value));
                }
            }
        }
    }

}
