package manage.hook;

import manage.oidc.OpenIdConnect;
import manage.repository.MetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class MetaDataHookConfiguration {

    @Bean
    @Autowired
    CompositeMetaDataHook hooks(MetaDataRepository metaDataRepository, OpenIdConnect openIdConnect) {
        EntityIdReconcilerHook entityIdReconcilerHook = new EntityIdReconcilerHook(metaDataRepository);
        OpenIdConnectHook openIdConnectHook = new OpenIdConnectHook(openIdConnect);

        return new CompositeMetaDataHook(Arrays.asList(entityIdReconcilerHook, openIdConnectHook));
    }


}
