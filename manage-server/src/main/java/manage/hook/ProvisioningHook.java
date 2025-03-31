package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static manage.model.EntityType.IDP;

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
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        String previousProvisioningType = (String) previous.metaDataFields().get("provisioning_type");
        String newProvisioningType = (String) newMetaData.metaDataFields().get("provisioning_type");
        if (!previousProvisioningType.equals(newProvisioningType)) {
            Schema schema = metaDataAutoConfiguration.schema(EntityType.PROV.getType());
            throw new ValidationException(
                    schema,
                    String.format("Not allowed the change the provisioning_type for provisioning metadata (changed from %s to %s)." +
                                    " Delete this entity and create new provisioning",
                            previousProvisioningType, newProvisioningType),
                    "metaDataFields.provisioning_type");
        }
        prePost(newMetaData, user);
        return this.filterInvalidApplications(newMetaData);
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        validate(metaData);
        validateScimIdentifier(metaData);
        return this.filterInvalidApplications(metaData);
    }

    @SuppressWarnings("unchecked")
    private void validate(MetaData newMetaData) {
        Map<String, Object> metaDataFields = newMetaData.metaDataFields();
        List<ValidationException> failures = new ArrayList<>();
        Schema schema = metaDataAutoConfiguration.schema(EntityType.PROV.getType());
        String provisioningType = (String) metaDataFields.get("provisioning_type");
        Map.of(
                "scim", List.of("scim_url"),
                "graph", List.of("graph_client_id", "graph_secret", "graph_tenant"),
                "eva", List.of("eva_url", "eva_token")
        ).forEach((type, required) -> {
            if (type.equals(provisioningType)) {
                required.forEach(attribute -> {
                    if (!StringUtils.hasText((String) metaDataFields.get(attribute))) {
                        failures.add(new ValidationException(schema,
                                String.format("%s is required with provisioningType %s", attribute, provisioningType), attribute));
                    }
                });
            }
        });
        if (provisioningType.equals("scim")) {
            if (!StringUtils.hasText((String) metaDataFields.get("scim_bearer_token"))) {
                List.of("scim_user", "scim_password").forEach(attribute -> {
                    if (!StringUtils.hasText((String) metaDataFields.get(attribute))) {
                        failures.add(new ValidationException(schema,
                                String.format("%s is required with provisioningType scim when no scim_bearer_token is configured",
                                        attribute), attribute));
                    }
                });

            }
        }
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

    private void validateScimIdentifier(MetaData newMetaData) {
        Map<String, Object> metaDataFields = newMetaData.metaDataFields();
        String provisioningType = (String) metaDataFields.get("provisioning_type");
        String scimUserIdentifier = (String) metaDataFields.get("scim_user_identifier");
        if ("scim".equals(provisioningType) && "eduID".equals(scimUserIdentifier)) {
            //It is required that the coin:institution_guid is specified and points to an existing IdP
            String institutionGuid = (String) metaDataFields.get("coin:institution_guid");
            if (!StringUtils.hasText(institutionGuid)) {
                Schema schema = metaDataAutoConfiguration.schema(EntityType.PROV.getType());
                throw new ValidationException(
                        schema,
                        "coin:institution_guid is required, for scim provisioning with an eduID scim_user_identifier.");
            } else {
                List<MetaData> references = metaDataRepository.findRaw(IDP.getType(),
                        String.format("{\"data.metaDataFields.coin:institution_guid\" : \"%s\"}", institutionGuid));
                if (references.isEmpty()) {
                    Schema schema = metaDataAutoConfiguration.schema(EntityType.PROV.getType());
                    throw new ValidationException(
                            schema,
                            "coin:institution_guid must be a valid / existing IdP institution_guid.");
                }
            }
        }
    }


}
