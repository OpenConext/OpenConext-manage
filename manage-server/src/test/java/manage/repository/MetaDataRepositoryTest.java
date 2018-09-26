package manage.repository;

import org.junit.Test;

import static org.junit.Assert.*;

public class MetaDataRepositoryTest {

    private MetaDataRepository subject = new MetaDataRepository(null);

    @Test
    public void escapeSpecialChars() {
        String result = subject.escapeSpecialChars("query (((test))) | part [test] ? {} + *");
        assertEquals("query \\(\\(\\(test\\)\\)\\) \\| part \\[test\\] \\? \\{\\} \\+ \\*", result);
    }
}