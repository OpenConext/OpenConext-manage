package manage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.common.mapper.TypeRef;
import manage.api.APIUser;
import manage.api.Scope;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public interface TestUtils {

    default boolean listOfMapsContainsValue(List<Map> l, Object o) {
        return l.stream().anyMatch(m -> m.containsValue(o));
    }

    default String readFile(String path) {
        try {
            return IOUtils.toString(new ClassPathResource(path).getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<>() {
    };

    TypeRef<List<Map<String, Object>>> mapListTypeRef = new TypeRef<>() {
    };

    ObjectMapper objectMapper = ObjectMapperWrapper.init();

    class ObjectMapperWrapper {
        private static com.fasterxml.jackson.databind.ObjectMapper init() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
            return objectMapper;
        }
    }

    default APIUser apiUser() {
        return new APIUser("test", List.of(Scope.SUPER_USER));
    }

}
