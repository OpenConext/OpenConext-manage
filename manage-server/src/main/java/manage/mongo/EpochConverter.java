package manage.mongo;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;

import java.time.Instant;

@ReadingConverter
public class EpochConverter implements Converter<Long, Instant> {

    @Override
    public Instant convert(Long epochMilliseconds) {
        return Instant.ofEpochMilli(epochMilliseconds);
    }
}
