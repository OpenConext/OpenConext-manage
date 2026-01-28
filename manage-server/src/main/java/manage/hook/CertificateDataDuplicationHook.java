package manage.hook;

import manage.api.AbstractUser;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class CertificateDataDuplicationHook extends MetaDataHookAdapter {

    private final MetaDataAutoConfiguration metaDataAutoConfiguration;

    public CertificateDataDuplicationHook(MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.metaDataAutoConfiguration = metaDataAutoConfiguration;
    }

    @Override
    public boolean appliesForMetaData(MetaData metaData) {
        String type = metaData.getType();
        return Stream.of(EntityType.IDP, EntityType.SP, EntityType.SRAM)
            .anyMatch(entityType -> entityType.getType().equals(type));
    }

    @Override
    public MetaData prePut(MetaData previous, MetaData newMetaData, AbstractUser user) {
        validate(newMetaData);
        return super.prePut(previous, newMetaData, user);
    }

    @Override
    public MetaData prePost(MetaData metaData, AbstractUser user) {
        validate(metaData);
        return super.prePost(metaData, user);
    }

    private void validate(MetaData metaData) {
        Map<String, Object> metaDataFields = metaData.metaDataFields();
        List<String> nonEmptyCerts = Stream.of("certData", "certData2", "certData3")
            .map(key -> (String) metaDataFields.get(key))
            .filter(StringUtils::hasText)
            .toList();

        Set<String> uniqueCerts = new HashSet<>(nonEmptyCerts);
        if (nonEmptyCerts.size() != uniqueCerts.size()) {
            List<ValidationException> failures = new ArrayList<>();
            Schema schema = metaDataAutoConfiguration.schema(metaData.getType());
            failures.add(new ValidationException(schema,
                "Certificate data fields must not contain duplicate values. Fields: \"certData\" \"certData2\" \"certData3\"",
                "certData"));
            ValidationException.throwFor(schema, failures);
        }
    }
}
