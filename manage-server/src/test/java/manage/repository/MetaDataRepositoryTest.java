package manage.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MetaDataRepositoryTest {

    private MetaDataRepository subject = new MetaDataRepository(null, "en,nl,pt");

    @Test
    public void escapeSpecialChars() {
        String result = subject.escapeSpecialChars("query (((test))) | part [test] ? {} + *");
        assertEquals("query \\(\\(\\(test\\)\\)\\) \\| part \\[test\\] \\? \\{\\} \\+ \\*", result);
    }
}