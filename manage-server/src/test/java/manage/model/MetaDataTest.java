package manage.model;

import manage.TestUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class MetaDataTest implements TestUtils{

    private MetaData subject;

    @Before
    public void before() throws IOException {
        subject =   objectMapper.readValue(readFile("json/meta_data_detail.json"), MetaData.class);;
    }

    @Test
    public void revision() throws Exception {
        subject.revision("new_id");


    }

    @Test
    public void promoteToLatest() throws Exception {
    }

    @Test
    public void merge() throws Exception {
    }

    @Test
    public void equals() throws Exception {
    }

}