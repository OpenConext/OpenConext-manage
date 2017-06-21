package mr.model;

import lombok.Getter;
import org.springframework.data.annotation.Id;

import java.io.Serializable;

@Getter
public class MetaData implements Serializable {

    @Id
    private String id;

    private String type;

    private Object data;

    public void setId(String id) {
        this.id = id;
    }
}
