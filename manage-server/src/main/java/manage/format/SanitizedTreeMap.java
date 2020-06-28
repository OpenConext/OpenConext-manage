package manage.format;

import org.springframework.util.StringUtils;

import java.util.TreeMap;

public class SanitizedTreeMap<K, V> extends TreeMap<K, V> {

    @Override
    public V put(K key, V value) {
        if (value instanceof String && StringUtils.hasText((String) value)) {
            value = (V) ((String) value).replaceAll("[\\n\\r\\t]", "").trim();
        }
        return super.put(key, value);
    }
}
