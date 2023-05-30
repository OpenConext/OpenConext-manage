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
        String provisioningType = (String) metaDataFields.get("provisioning_type");
        if ("scim".equals(provisioningType) && !metaDataFields.containsKey("scim_url")) {
            failures.add(new ValidationException(schema, "SCIM URL is required with provisioningType SCIM", "scim_url"));
        } else if ("mail".equals(provisioningType) && !metaDataFields.containsKey("provisioning_mail")) {
            failures.add(new ValidationException(schema, "Provisioning mail is required with provisioningType mail", "provisioning_mail"));
        } else if ("eva".equals(provisioningType) && !metaDataFields.containsKey("eva_token")) {
            failures.add(new ValidationException(schema, "EVA token is required with provisioningType eva", "eva_token"));
        }
        ValidationException.throwFor(schema, failures);
    }

    private MetaData filterInvalidApplications(MetaData metaData) {
        List<Map<String, String>> applications = (List<Map<String, String>>) metaData.getData().getOrDefault("applications", Collections.emptyList());
        List<Map<String, String>> newApplications = applications.stream().filter(application -> metaDataRepository.findById(application.get("id"), application.get("type")) != null).collect(toList());
        metaData.getData().put("applications", newApplications);
        return metaData;
    }



}
