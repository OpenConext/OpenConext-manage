package manage.mongo;

import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.*;

public class EpochConverterTest {

    private EpochConverter epochConverter = new EpochConverter();

    @Test
    public void convert() {
        Instant instant = epochConverter.convert(1580133325454L);
        assertEquals("2020-01-27T13:55:25.454Z", instant.toString());
    }
}