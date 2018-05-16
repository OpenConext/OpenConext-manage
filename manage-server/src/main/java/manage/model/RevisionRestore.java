package manage.model;

import lombok.Getter;

import javax.validation.constraints.NotNull;

@Getter
public class RevisionRestore {

    @NotNull
    private String id;
    @NotNull
    private String type;
    @NotNull
    private String parentType;

}
