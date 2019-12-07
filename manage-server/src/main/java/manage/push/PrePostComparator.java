package manage.push;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toList;

public class PrePostComparator {

    public Set<Delta> compare(List<Map<String, Object>> preProvidersData, List<Map<String, Object>> postProvidersData) {
        Set<Delta> deltas = new HashSet<>();
        doCompare(deltas, preProvidersData, postProvidersData, false);
        doCompare(deltas, postProvidersData, preProvidersData, true);
        return deltas;
    }

    private void doCompare(Set<Delta> deltas, List<Map<String, Object>> firstProvidersData, List<Map<String, Object>>
            secondProvidersData, boolean reversed) {
        firstProvidersData.forEach(provider -> compareProvider(deltas, provider,
                this.findByEntityIdAndType(secondProvidersData,
                        String.class.cast(provider.get("entity_id")),
                        String.class.cast(provider.get("type"))), reversed));
    }

    private void compareProvider(Set<Delta> deltas, Map<String, Object> provider,
                                 Optional<Map<String, Object>> optionalProvider, boolean reversed) {
        Map<String, Object> otherProvider = optionalProvider.orElse(new HashMap<>());
        this.compareProvider(deltas, provider, otherProvider, reversed);

    }

    private void compareProvider(Set<Delta> deltas, Map<String, Object> provider, Map<String, Object> otherProvider,
                                 boolean reversed) {
        provider.forEach((key, value) -> {
            Object otherValue = otherProvider.get(key);
            boolean stringNotEquals = value instanceof String && otherValue instanceof String &&
                    !String.class.cast(((String) value).trim()).equals(String.class.cast(((String) otherValue).trim()));
            if (stringNotEquals || !Objects.equals(value, otherValue)) {
                deltas.add(new Delta(
                        String.class.cast(provider.get("entity_id")),
                        key,
                        reversed ? otherValue : value,
                        reversed ? value : otherValue));
            }
        });
    }

    private Optional<Map<String, Object>> findByEntityIdAndType(List<Map<String, Object>> providersData,
                                                                String entityId, String type) {
        List<Map<String, Object>> providers = providersData.stream().filter(provider -> provider.get("entity_id")
                .equals(entityId) && provider.get("type").equals(type)).collect(toList());
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

}
