package manage.hook;

import crypto.HybridRSAKeyStore;
import crypto.KeyStore;
import lombok.SneakyThrows;
import manage.conf.MetaDataAutoConfiguration;
import manage.repository.MetaDataRepository;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.nio.charset.Charset;
import java.util.Arrays;

@Configuration
public class MetaDataHookConfiguration {

    @Bean
    @SneakyThrows
    CompositeMetaDataHook hooks(MetaDataRepository metaDataRepository,
                                MetaDataAutoConfiguration metaDataAutoConfiguration,
                                @Value("${crypto.public-key-location}") Resource publicKeyResource,
                                @Value("${crypto.development-mode}") Boolean developmentMode,
                                @Value("${crypto.enabled}") boolean cryptoEnabled,
                                @Value("${feature_toggles.allow_secret_public_rp}") boolean allowSecretPublicRP) {

        KeyStore keyStore = developmentMode ? new HybridRSAKeyStore() :
            new HybridRSAKeyStore(IOUtils.toString(publicKeyResource.getInputStream(), Charset.defaultCharset()), true);

        return new CompositeMetaDataHook(
            Arrays.asList(
                new SecurityHook(),
                new EmptyRevisionHook(metaDataAutoConfiguration),
                new EntityIdDuplicationHook(metaDataAutoConfiguration, metaDataRepository),
                new IdentityProviderDeleteHook(metaDataAutoConfiguration, metaDataRepository),
                new ServiceProviderDeleteHook(metaDataAutoConfiguration, metaDataRepository),
                new PolicyNameConstraintsHook(metaDataAutoConfiguration, metaDataRepository),
                new PolicyValidationHook(metaDataAutoConfiguration),
                new ExtraneousKeysPoliciesHook(metaDataAutoConfiguration),
                new OidcValidationHook(metaDataAutoConfiguration, allowSecretPublicRP),
                new TypeSafetyHook(metaDataAutoConfiguration),
                new EntityIdConstraintsHook(metaDataRepository),
                new EntityIdReconcilerHook(metaDataRepository),
                new SSIDValidationHook(metaDataRepository, metaDataAutoConfiguration),
                new SecretHook(metaDataAutoConfiguration),
                new RequiredAttributesHook(metaDataAutoConfiguration),
                new ProvisioningHook(metaDataRepository, metaDataAutoConfiguration),
                new EncryptionHook(keyStore, cryptoEnabled),
                new ProvisioningApplicationDeletionHook(metaDataRepository),
                new IdentityProviderBrinCodeHook(metaDataAutoConfiguration),
                new CertificateDataDuplicationHook(metaDataAutoConfiguration),
                new OrganisationDeletionHook(metaDataRepository, metaDataAutoConfiguration)));
    }


}
