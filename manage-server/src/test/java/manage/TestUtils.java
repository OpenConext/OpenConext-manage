package manage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

public interface TestUtils {

    default String readFile(String path) {
        try {
            return IOUtils.toString(new ClassPathResource(path).getInputStream(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    TypeReference<Map<String, Object>> mapTypeRef = new TypeReference<Map<String, Object>>() {
    };

    ObjectMapper objectMapper = ObjectMapperWrapper.init();

    class ObjectMapperWrapper {
        private static com.fasterxml.jackson.databind.ObjectMapper init() {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules();
            return objectMapper;
        }
    }
}
