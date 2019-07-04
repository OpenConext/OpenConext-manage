package manage.hook;

import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TypeSafetyHook extends MetaDataHookAdapter {

    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    public TypeSafetyHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    @SuppressWarnings("unchecked")
    public MetaData preValidate(MetaData metaData) {
        Map<String, Object> schema = this.metaDataAutoConfiguration.schemaRepresentation(EntityType.fromType(metaData.getType()));
        Map<String, Object> schemaMetaDataFields = Map.class.cast(Map.class.cast(schema.get("properties")).get("metaDataFields"));

        List<String> booleanPatternKeys = typedProperties(schemaMetaDataFields, "boolean", "patternProperties");
        List<String> numberPatternKeys = typedProperties(schemaMetaDataFields, "number", "patternProperties");

        List<String> booleanKeys = typedProperties(schemaMetaDataFields, "boolean", "properties");
        List<String> numberKeys = typedProperties(schemaMetaDataFields, "number", "properties");

        Map<String, Object> metaDataFields = metaData.metaDataFields();
        metaDataFields.forEach((key, value) -> {
            if ((propertyMatches(key, booleanKeys) || patternPopertyMatches(key, booleanPatternKeys)) && value instanceof String) {
                metaDataFields.put(key, "1".equals(value));
            }
            if ((propertyMatches(key, numberKeys) || patternPopertyMatches(key, numberPatternKeys)) && value instanceof String) {
                metaDataFields.put(key, Integer.parseInt((String) value));
            }
        });
        return metaData;
    }

    private boolean propertyMatches(String key, List<String> properties) {
        return properties.contains(key);
    }

    private boolean patternPopertyMatches(String key, List<String> patternProperties) {
        return patternProperties.stream().filter(prop -> Pattern.compile(prop).matcher(key).matches()).count() > 0;
    }

    private List<String> typedProperties(Map metaDataFields, String type, String name) {
        Map<String, Map<String, Object>> patternProperties = (Map<String, Map<String, Object>>) metaDataFields.get(name);
        return patternProperties.entrySet().stream().filter(e -> e.getValue().containsKey("type") && e.getValue().get("type").equals(type)).map(e -> e.getKey()).collect(Collectors.toList());
    }

}
