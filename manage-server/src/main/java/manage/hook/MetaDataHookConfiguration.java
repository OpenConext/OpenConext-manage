package manage.hook;

import manage.conf.MetaDataAutoConfiguration;
import manage.repository.MetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class MetaDataHookConfiguration {

    @Bean
    @Autowired
    CompositeMetaDataHook hooks(MetaDataRepository metaDataRepository,
                                MetaDataAutoConfiguration metaDataAutoConfiguration) {

        EmptyRevisionHook emptyRevisionHook = new EmptyRevisionHook(metaDataAutoConfiguration);
        EntityIdReconcilerHook entityIdReconcilerHook = new EntityIdReconcilerHook(metaDataRepository);
        SecretHook secretHook = new SecretHook(metaDataAutoConfiguration);
        TypeSafetyHook typeSafetyHook = new TypeSafetyHook(metaDataAutoConfiguration);
        EntityIdConstraintsHook entityIdConstraintsHook = new EntityIdConstraintsHook(metaDataRepository);
        OidcValidationHook validationHook = new OidcValidationHook(metaDataAutoConfiguration);
        SSIDValidationHook ssidValidationHook = new SSIDValidationHook(metaDataRepository, metaDataAutoConfiguration);
        RequiredAttributesHook requiredAttributesHook = new RequiredAttributesHook(metaDataAutoConfiguration);
        ProvisioningHook provisioningHook = new ProvisioningHook(metaDataRepository, metaDataAutoConfiguration);

        return new CompositeMetaDataHook(
                Arrays.asList(
                        emptyRevisionHook,
                        validationHook,
                        typeSafetyHook,
                        entityIdConstraintsHook,
                        entityIdReconcilerHook,
                        ssidValidationHook,
                        secretHook,
                        requiredAttributesHook,
                        provisioningHook));
    }


}
