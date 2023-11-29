package manage.hook;

import crypto.KeyStore;
import lombok.SneakyThrows;
import manage.conf.MetaDataAutoConfiguration;
import manage.repository.MetaDataRepository;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.nio.charset.Charset;
import java.util.Arrays;

@Configuration
public class MetaDataHookConfiguration {

    @Bean
    @Autowired
    @SneakyThrows
    CompositeMetaDataHook hooks(MetaDataRepository metaDataRepository,
                                MetaDataAutoConfiguration metaDataAutoConfiguration,
                                @Value("${crypto.public-key-location}") Resource publicKeyResource,
                                @Value("${crypto.development-mode}") Boolean developmentMode) {

        EmptyRevisionHook emptyRevisionHook = new EmptyRevisionHook(metaDataAutoConfiguration);
        EntityIdReconcilerHook entityIdReconcilerHook = new EntityIdReconcilerHook(metaDataRepository);
        SecretHook secretHook = new SecretHook(metaDataAutoConfiguration);
        TypeSafetyHook typeSafetyHook = new TypeSafetyHook(metaDataAutoConfiguration);
        EntityIdConstraintsHook entityIdConstraintsHook = new EntityIdConstraintsHook(metaDataRepository);
        OidcValidationHook validationHook = new OidcValidationHook(metaDataAutoConfiguration);
        SSIDValidationHook ssidValidationHook = new SSIDValidationHook(metaDataRepository, metaDataAutoConfiguration);
        RequiredAttributesHook requiredAttributesHook = new RequiredAttributesHook(metaDataAutoConfiguration);
        ProvisioningHook provisioningHook = new ProvisioningHook(metaDataRepository, metaDataAutoConfiguration);
        KeyStore keyStore = developmentMode ? new KeyStore() :
                new KeyStore(IOUtils.toString(publicKeyResource.getInputStream(), Charset.defaultCharset()), true);
        EncryptionHook encryptionHook = new EncryptionHook(keyStore);

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
                        provisioningHook,
                        encryptionHook));
    }


}
