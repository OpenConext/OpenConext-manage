package manage.format;

import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class SanitizedTreeMapTest {

    @Test
    public void put() {
        Map<String, String> subject = new SanitizedTreeMap<>();
        subject.put("key","\n\t               value    \r");
        assertEquals("value", subject.get("key"));

        Map<String, String> other = new SanitizedTreeMap<>();
        other.put("other", "other-value");
        subject.putAll(other);

        other.clear();
        assertEquals("other-value", subject.get("other"));

    }
}