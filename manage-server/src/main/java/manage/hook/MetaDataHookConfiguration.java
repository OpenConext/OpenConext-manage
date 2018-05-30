package manage.hook;

import manage.repository.MetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;

@Configuration
public class MetaDataHookConfiguration {
    
    @Bean
    CompositeMetaDataHook hooks(@Autowired MetaDataRepository metaDataRepository) {
        EntityIdReconcilerHook hook = new EntityIdReconcilerHook();
        hook.setMetaDataRepository(metaDataRepository);
        return new CompositeMetaDataHook(Arrays.asList(hook));
    }

    
}
