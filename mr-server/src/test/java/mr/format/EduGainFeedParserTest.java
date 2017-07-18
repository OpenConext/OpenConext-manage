package mr.format;

import org.junit.Test;
import org.springframework.core.io.Resource;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EduGainFeedParserTest {
    @Test
    public void parse() throws Exception {
        EduGainFeedParser eduGainFeedParser = new EduGainFeedParser(new GZIPClassPathResource("/xml/eduGain.xml.gz"));
        List<String> entities = eduGainFeedParser.parse();
        System.out.println(entities.size());
    }

}