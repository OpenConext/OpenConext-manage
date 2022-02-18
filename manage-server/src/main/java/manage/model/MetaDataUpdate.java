package manage.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MetaDataUpdate implements PathUpdates {

    @Id
    private String id;

    @NotNull
    private String type;

    @NotNull
    private Map<String, Object> pathUpdates;

    private Map<String, Object> externalReferenceData;

    @Override
    public String getMetaDataId() {
        return id;
    }
}
