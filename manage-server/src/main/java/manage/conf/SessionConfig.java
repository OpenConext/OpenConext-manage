package manage.conf;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.mongo.JdkMongoSessionConverter;
import org.springframework.session.data.mongo.config.annotation.web.http.EnableMongoHttpSession;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

import java.time.Duration;

@Configuration
@EnableMongoHttpSession
public class SessionConfig extends AbstractHttpSessionApplicationInitializer {

    @Bean
    CookieSerializer cookieSerializer(@Value("${secure_cookie}") boolean secureCookie) {
        DefaultCookieSerializer defaultCookieSerializer = new DefaultCookieSerializer();
        defaultCookieSerializer.setSameSite("None");
        defaultCookieSerializer.setUseSecureCookie(secureCookie);
        return defaultCookieSerializer;
    }

    @Bean
    public JdkMongoSessionConverter jdkMongoSessionConverter() {
        return new JdkMongoSessionConverter(Duration.ofHours(8));
    }
//    @Bean
//    JacksonMongoSessionConverter mongoSessionConverter() {
//        SimpleModule module = new CoreJackson2Module() {
//            @Override
//            public void setupModule(SetupContext context) {
//                super.setupModule(context);
//                context.setMixInAnnotations(FederatedUser.class, FederatedUserMixin.class);
//                context.setMixInAnnotations(HashSet.class, HashSetMixin.class);
//                context.setMixInAnnotations(LinkedHashMap.class, LinkedHashMapMixin.class);
//            }
//        };
//
//        List<Module> modules = new ArrayList<>();
//        modules.add(module);
//
//        return new JacksonMongoSessionConverter(modules);
//    }
//
//    private static class FederatedUserMixin {
//    }
//
//    private static class HashSetMixin {
//    }
//
//    private static class LinkedHashMapMixin {
//    }
//

}
