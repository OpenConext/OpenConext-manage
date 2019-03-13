package manage.format;

import manage.model.EntityType;
import manage.model.MetaData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.springframework.util.StringUtils.hasText;

/**
 * Mimics the parsing of metadata from the
 * https://github.com/OpenConext/OpenConext-engineblock-metadata/blob/master/src/Entity/Assembler
 * /JanusPushMetadataAssembler.php
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
        commonAttributes.put("metadata:coin:publish_in_edugain", empty());

        commonAttributes.put("metadata:certData", empty());
        commonAttributes.put("metadata:certData2", empty());
        commonAttributes.put("metadata:certData3", empty());

        commonAttributes.put("state", empty());
        //contact persons are handled in separate method
        commonAttributes.put("metadata:NameIDFormat", empty());
        //single log outs are handled in separate method
        commonAttributes.put("metadata:coin:disable_scoping", empty());
        commonAttributes.put("metadata:coin:additional_logging", empty());
        commonAttributes.put("metadata:coin:signature_method", empty());
        commonAttributes.put("manipulation", of("manipulation_code"));

        spAttributes.put("metadata:coin:transparant_issuer", empty());
        spAttributes.put("metadata:coin:trusted_proxy", empty());
        spAttributes.put("metadata:coin:requesterid_required", empty());
        spAttributes.put("metadata:coin:display_unconnected_idps_wayf", empty());

        spAttributes.put("metadata:coin:eula", empty());
        spAttributes.put("metadata:coin:do_not_add_attribute_aliases", empty());
        spAttributes.put("metadata:coin:policy_enforcement_decision_required", empty());
        spAttributes.put("metadata:coin:attribute_aggregation_required", empty());
        spAttributes.put("metadata:coin:no_consent_required", empty());

        idpAttributes.put("metadata:coin:guest_qualifier", empty());
        idpAttributes.put("metadata:coin:schachomeorganization", empty());
        idpAttributes.put("metadata:coin:hidden", empty());
    }

    public Map<String, Object> parseServiceProvider(MetaData metaDataContainer) {
        Map<String, Object> source = metaDataContainer.getData();

        Map<String, Object> serviceProvider = new TreeMap<>();
        serviceProvider.put("type", EntityType.SP.getJanusDbValue());

        addCommonProviderAttributes(source, serviceProvider);
        addNameIDFormats(source, serviceProvider);
        addAttributeReleasePolicy(source, serviceProvider);
        addAssertionConsumerService(source, serviceProvider);

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

        addCommonProviderAttributes(source, identityProvider);
        addSingleSignOnService(source, identityProvider);

        idpAttributes.forEach((key, value) -> this.addToResult(source, identityProvider, key, value));

        addShibMdScopes(source, identityProvider);

        removeEmptyValues(identityProvider);
        return identityProvider;
    }

    private void addCommonProviderAttributes(Map<String, Object> source, Map<String, Object> result) {
        commonAttributes.forEach((key, value) -> this.addToResult(source, result, key, value));
        addLogo(source, result);
        addContactPersons(source, result);
        addSingleLogOutService(source, result);
        addRedirectSign(source, result);

        List<Map<String, String>> allowedEntities = (List<Map<String, String>>) source.get("allowedEntities");
        result.put("allowed_connections", allowedEntities == null ? new ArrayList<>() : allowedEntities);
        result.put("allow_all_entities", source.get("allowedall"));
    }

    private void addLogo(Map<String, Object> source, Map<String, Object> result) {
        result = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new TreeMap<>());
        Map<String, String> metaDataFields = (Map<String, String>) source.get("metaDataFields");

        String height = metaDataFields.get("logo:0:height");
        String url = metaDataFields.get("logo:0:url");
        String width = metaDataFields.get("logo:0:width");

        if (hasText(height) || hasText(url) || hasText(width)) {
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
        Map<String, String> metaDataFields = (Map<String, String>) source.get("metaDataFields");
        IntStream.range(0, 4).forEach(i -> {
            String contactType = metaDataFields.get("contacts:" + i + ":contactType");
            String emailAddress = metaDataFields.get("contacts:" + i + ":emailAddress");
            String telephoneNumber = metaDataFields.get("contacts:" + i + ":telephoneNumber");
            String givenName = metaDataFields.get("contacts:" + i + ":givenName");
            String surName = metaDataFields.get("contacts:" + i + ":surName");

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

    private void putIfHasText(String key, String value, Map<String, String> result) {
        if (hasText(value)) {
            result.put(key, value);
        }
    }

    private void addSingleLogOutService(Map<String, Object> source, Map<String, Object> result) {
        result = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new TreeMap<>());
        Map<String, String> metaDataFields = (Map<String, String>) source.get("metaDataFields");

        String location = metaDataFields.get("SingleLogoutService_Location");
        String binding = metaDataFields.get("SingleLogoutService_Binding");
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
        Map<String, String> metaDataFields = (Map<String, String>) source.get("metaDataFields");

        String redirectSign = metaDataFields.get("redirect.sign");
        if (hasText(redirectSign)) {
            Map<String, Boolean> redirect = new HashMap<>();
            redirect.put("sign", redirectSign.equalsIgnoreCase("1"));
            result.put("redirect", redirect);
        }
    }

    private void addNameIDFormats(Map<String, Object> source, Map<String, Object> result) {
        final Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new
            TreeMap<>());
        Map<String, String> metaDataFields = (Map<String, String>) source.get("metaDataFields");

        IntStream.range(0, 3).forEach(i -> {
            String nameIdFormat = metaDataFields.get("NameIDFormats:" + i);
            if (hasText(nameIdFormat)) {
                Set<String> nameIDFormats = (Set<String>) metadata.computeIfAbsent(
                    "NameIDFormats", key -> new HashSet<>());
                nameIDFormats.add(nameIdFormat);
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
        if (enabled != null && Boolean.class.cast(enabled)) {
            Object possibleAttributes = arp.get("attributes");
            if (possibleAttributes != null && possibleAttributes instanceof List) {
                List<String> listAttributes = (List<String>) possibleAttributes;
                result.put("arp_attributes", listAttributes);

            } else if (possibleAttributes != null && possibleAttributes instanceof Map) {
                Map<String, List<Map<String, String>>> attributes = (Map<String, List<Map<String, String>>>) possibleAttributes;

                //bugfix for EB not having the knowledge that 'idp' source is special
                Collection<List<Map<String, String>>> values = attributes.values();
                values.forEach(arpValues -> arpValues.forEach(map -> map.entrySet()
                    .removeIf(entry -> entry.getKey().equals("source") && entry.getValue().equals("idp"))));

                result.put("arp_attributes", attributes);

            }
        }
    }

    private void addSingleSignOnService(Map<String, Object> source, Map<String, Object> result) {
        final Map<String, Object> metadata = (Map<String, Object>) result.computeIfAbsent("metadata", key -> new
            TreeMap<>());
        Map<String, String> metaDataFields = (Map<String, String>) source.get("metaDataFields");
        IntStream.range(0, 10).forEach(i -> {
            String binding = metaDataFields.get("SingleSignOnService:" + i + ":Binding");
            String location = metaDataFields.get("SingleSignOnService:" + i + ":Location");

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
        IntStream.range(0, 5).forEach(i -> {
            String allowed = String.class.cast(metaDataFields.get("shibmd:scope:" + i + ":allowed"));
            String regexp = String.class.cast(metaDataFields.get("shibmd:scope:" + i + ":regexp"));

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
        Map<String, String> metaDataFields = (Map<String, String>) source.get("metaDataFields");
        IntStream.range(0, 30).forEach(i -> {
            String binding = metaDataFields.get("AssertionConsumerService:" + i + ":Binding");
            String location = metaDataFields.get("AssertionConsumerService:" + i + ":Location");
            String index = metaDataFields.get("AssertionConsumerService:" + i + ":index");

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

    protected void addToResult(Map<String, Object> source,
                               Map<String, Object> result,
                               String compoundName,
                               Optional<String> convertTo) {
        List<String> parts = Arrays.asList(compoundName.split(":"));
        if (parts.size() == 1) {
            Object o = source.get(compoundName);
            if (o != null) {
                result.put(convertTo.orElse(compoundName), o);
            }
            return;
        }
        Iterator<String> iterator = parts.iterator();
        String value = null;
        while (iterator.hasNext()) {
            String part = iterator.next();
            if (part.equals("metadata")) {
                result = (Map<String, Object>) result.computeIfAbsent(part, key -> new TreeMap<String, Map<String,
                    Object>>());
                value = (String) ((Map) source.get("metaDataFields")).get(compoundName.substring(BEGIN_INDEX));
            } else {
                if (iterator.hasNext()) {
                    result = (Map<String, Object>) result.computeIfAbsent(part, key -> new TreeMap<String,
                        Map<String, Object>>());
                } else if (value != null) {
                    result.put(convertTo.orElse(part), value);
                }
            }
        }
    }

}
