package manage.format;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

public class GZIPClassPathResource extends ClassPathResource{

    public GZIPClassPathResource(String path) {
        super(path);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new GZIPInputStream(super.getInputStream());
    }
}
