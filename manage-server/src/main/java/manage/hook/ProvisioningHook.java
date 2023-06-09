package manage.hook;

import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static manage.model.EntityType.*;

@SuppressWarnings("unchecked")
public class ProvisioningHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;
    private final MetaDataRepository metaDataRepository;

    public ProvisioningHook(MetaDataRepository metaDataRepository, MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
        this.metaDataRepository = metaDataRepository;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        return metaData.getType().equals(EntityType.PROV.getType());
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData) {
        String previousProvisioningType = (String) previous.metaDataFields().get("provisioning_type");
        String newProvisioningType = (String) previous.metaDataFields().get("provisioning_type");
        if (!previousProvisioningType.equals(newProvisioningType)) {
            Schema schema = metaDataAutoConfiguration.schema(EntityType.PROV.getType());
            throw new ValidationException(
                    schema,
                    String.format("Not allowed the change the provisioning_type for provisioning metadata (changed from %s to %s)",
                            previousProvisioningType, newProvisioningType),
                    "metaDataFields.provisioning_type");
        }
        prePost(newMetaData);
        return this.filterInvalidApplications(newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData) {
        validate(metaData);
        return this.filterInvalidApplications(metaData);
    }

    @SuppressWarnings("unchecked")
    private void validate(MetaData newMetaData) {
        Map<String, Object> metaDataFields = newMetaData.metaDataFields();
        List<ValidationException> failures = new ArrayList<>();
        Schema schema = metaDataAutoConfiguration.schema(EntityType.PROV.getType());
        Map.of(
                "scim", List.of("scim_url", "scim_user", "scim_password"),
                "graph", List.of("graph_url", "graph_token"),
                "eva", List.of("eva_url", "eva_token")
        ).forEach((provisioningType, required) -> {
            required.forEach(attribute -> {
                if (!StringUtils.hasText((String) metaDataFields.get(attribute))) {
                    failures.add(new ValidationException(schema,
                            String.format("%s is required with provisioningType %s", provisioningType, attribute), attribute));
                }
            });
        });
        ValidationException.throwFor(schema, failures);
    }

    private MetaData filterInvalidApplications(MetaData metaData) {
        List<Map<String, String>> applications = (List<Map<String, String>>) metaData.getData().getOrDefault("applications", Collections.emptyList());
        List<Map<String, String>> newApplications = applications.stream()
                .filter(application -> metaDataRepository.findById(application.get("id"), application.get("type")) != null)
                .collect(toList());
        metaData.getData().put("applications", newApplications);
        return metaData;
    }


}
