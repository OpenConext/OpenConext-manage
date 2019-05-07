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
        EntityIdReconcilerHook entityIdReconcilerHook = new EntityIdReconcilerHook(metaDataRepository);
        OpenIdConnectHook openIdConnectHook = new OpenIdConnectHook(openIdConnect, acsLocation);
        SecretHook secretHook = new SecretHook();
        TypeSafetyHook typeSafetyHook = new TypeSafetyHook(metaDataAutoConfiguration);
        return new CompositeMetaDataHook(Arrays.asList(typeSafetyHook, entityIdReconcilerHook, openIdConnectHook, secretHook));
    }


}
