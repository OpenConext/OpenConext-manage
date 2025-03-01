package manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MetaDataKeyDelete implements Serializable {

    @NotNull
    private String type;

    @NotNull
    private String metaDataKey;
}
