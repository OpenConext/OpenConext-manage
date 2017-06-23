package mr.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mr.mongo.MongobeeConfiguration;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetaData implements Serializable {

    @Id
    private String id;

    @NotNull
    private String type;

    private Revision revision;

    @NotNull
    private Object data;

    public MetaData(String type, Object data) {
        this.type = type;
        this.data = data;
    }

    public void initial(String id, String createdBy) {
        this.id = id;
        this.revision = new Revision(0, Instant.now(), null, createdBy);
    }

    public void revision(String newId) {
        this.type = this.type.concat(MongobeeConfiguration.REVISION_POSTFIX);
        this.revision.setParentId(this.id);
        this.id = newId;
    }

    public void promoteToLatest(String updatedBy) {
        this.revision = new Revision(revision.getNumber() + 1, Instant.now(), null, updatedBy);
    }
}
