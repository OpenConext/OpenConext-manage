package manage.model;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
public class Revision {

    private int number;
    private Instant created;
    private String parentId;
    private String updatedBy;
    private Instant terminated;

    public Revision(int number, Instant created, String parentId, String updatedBy) {
        this.number = number;
        this.created = created;
        this.parentId = parentId;
        this.updatedBy = updatedBy;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

    public void terminate() {
        this.terminated = Instant.now();
    }

    public void deTerminate(String newId) {
        this.terminated = null;
        this.parentId = newId;
    }
}
