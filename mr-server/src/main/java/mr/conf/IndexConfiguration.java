package mr.conf;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class IndexConfiguration {

    private String name;
    private String type;
    private List<String> fields;
    private boolean unique;


}
