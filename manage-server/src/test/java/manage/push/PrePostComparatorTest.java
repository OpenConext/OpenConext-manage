package manage.push;

import groovy.transform.stc.ClosureParams;
import manage.TestUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class PrePostComparatorTest implements TestUtils {

    private PrePostComparator subject = new PrePostComparator();

    @Test
    @SuppressWarnings("unchecked")
    public void testCompare() throws IOException {
        List<Map<String, Object>> prePush = objectMapper.readValue(readFile("push/pre.push.json"), List.class);
        List<Map<String, Object>> postPush = objectMapper.readValue(readFile("push/post.push.json"), List.class);

        Set<Delta> deltas = subject.compare(prePush, postPush);
        assertEquals(3, deltas.size());

        deltas.forEach(delta -> assertEquals("something_changed", delta.getPostPushValue()));
    }

}