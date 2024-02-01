package manage.hook;

import crypto.KeyStore;
import crypto.RSAKeyStore;
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

        KeyStore keyStore = developmentMode ? new RSAKeyStore() :
                new RSAKeyStore(IOUtils.toString(publicKeyResource.getInputStream(), Charset.defaultCharset()), true);

        return new CompositeMetaDataHook(
                Arrays.asList(
                        new SecurityHook(),
                        new EmptyRevisionHook(metaDataAutoConfiguration),
                        new ExtraneousKeysPoliciesHook(metaDataAutoConfiguration),
                        new OidcValidationHook(metaDataAutoConfiguration),
                        new TypeSafetyHook(metaDataAutoConfiguration),
                        new EntityIdConstraintsHook(metaDataRepository),
                        new EntityIdReconcilerHook(metaDataRepository),
                        new SSIDValidationHook(metaDataRepository, metaDataAutoConfiguration),
                        new SecretHook(metaDataAutoConfiguration),
                        new RequiredAttributesHook(metaDataAutoConfiguration),
                        new ProvisioningHook(metaDataRepository, metaDataAutoConfiguration),
                        new EncryptionHook(keyStore)));
    }


}
