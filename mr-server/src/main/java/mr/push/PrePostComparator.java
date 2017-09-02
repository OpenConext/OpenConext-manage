package mr.push;

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
        doCompare(deltas, preProvidersData, postProvidersData);
        doCompare(deltas, postProvidersData, preProvidersData);
        return deltas;
    }

    private void doCompare(Set<Delta> deltas, List<Map<String, Object>> firstProvidersData, List<Map<String, Object>> secondProvidersData) {
        firstProvidersData.forEach(provider -> compareProvider(deltas, provider, this.findByEntityId(secondProvidersData, String.class.cast(provider.get("entity_id")))));
    }

    private void compareProvider(Set<Delta> deltas, Map<String, Object> provider, Optional<Map<String, Object>> optionalProvider) {
        Map<String, Object> otherProvider = optionalProvider.orElse(new HashMap<>());
        this.compareProvider(deltas, provider, otherProvider);

    }

    private void compareProvider(Set<Delta> deltas, Map<String, Object> provider, Map<String, Object> otherProvider) {
        provider.forEach((key, value) -> {
            Object otherValue = otherProvider.get(key);
            if (!Objects.equals(value, otherValue)) {
                deltas.add(new Delta(String.class.cast(provider.get("entity_id")), key, value, otherValue));
            }
        });
    }

    private Optional<Map<String, Object>> findByEntityId(List<Map<String, Object>> providersData, String entityId) {
        List<Map<String, Object>> providers = providersData.stream().filter(provider -> provider.get("entity_id").equals(entityId)).collect(toList());
        return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
    }

}
