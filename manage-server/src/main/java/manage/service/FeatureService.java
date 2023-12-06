package manage.service;

import manage.conf.Features;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
public class FeatureService {

    private final List<Features> enabledFeatures;

    public FeatureService(@Value("${features}") String features) {
        List<String> allFeatures = Arrays.stream(Features.values())
                .map(Enum::name)
                .collect(toList());

        if (null != features && !features.isEmpty()) {
            this.enabledFeatures = Stream.of(features.split(","))
                    .filter(feature -> allFeatures.contains(feature.trim().toUpperCase()))
                    .map(feature -> Features.valueOf(feature.trim().toUpperCase()))
                    .collect(toList());
        } else {
            this.enabledFeatures = Collections.emptyList();
        }
    }

    public boolean isFeatureEnabled(Features feature) {
        return enabledFeatures.contains(feature);
    }

}
