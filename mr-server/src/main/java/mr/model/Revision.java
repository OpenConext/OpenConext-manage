package mr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Revision {

    private int number;
    private Instant created;
    private String parentId;
    private String updatedBy;

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
}
