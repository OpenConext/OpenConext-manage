package manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRequest {

    @NotNull
    private String id;

    @NotNull
    private String type;

    @NotNull
    private String metaDataId;

    private String revisionNotes;

}
