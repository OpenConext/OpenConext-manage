package manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequest {

    public ChangeRequest(String id, String type, String metaDataId) {
        this.id = id;
        this.type = type;
        this.metaDataId = metaDataId;
    }

    @NotNull
    private String id;

    @NotNull
    private String type;

    @NotNull
    private String metaDataId;

    private String revisionNotes;

}
