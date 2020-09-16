package manage.hook;

import manage.conf.MetaDataAutoConfiguration;
import manage.oidc.OpenIdConnect;
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
                                OpenIdConnect openIdConnect,
                                MetaDataAutoConfiguration metaDataAutoConfiguration,
                                @Value("${oidc.acsLocation}") String acsLocation) {

        EmptyRevisionHook emptyRevisionHook = new EmptyRevisionHook(metaDataAutoConfiguration);
        EntityIdReconcilerHook entityIdReconcilerHook = new EntityIdReconcilerHook(metaDataRepository);
        OpenIdConnectHook openIdConnectHook = new OpenIdConnectHook(openIdConnect, acsLocation);
        SecretHook secretHook = new SecretHook();
        TypeSafetyHook typeSafetyHook = new TypeSafetyHook(metaDataAutoConfiguration);
        EntityIdConstraintsHook entityIdConstraintsHook = new EntityIdConstraintsHook(metaDataRepository);
        OidcValidationHook validationHook = new OidcValidationHook(metaDataAutoConfiguration);
        SSIDValidationHook ssidValidationHook = new SSIDValidationHook(metaDataRepository, metaDataAutoConfiguration);

        return new CompositeMetaDataHook(
                Arrays.asList(
                        emptyRevisionHook,
                        validationHook,
                        typeSafetyHook,
                        entityIdConstraintsHook,
                        entityIdReconcilerHook,
                        openIdConnectHook,
                        ssidValidationHook,
                        secretHook));
    }


}
