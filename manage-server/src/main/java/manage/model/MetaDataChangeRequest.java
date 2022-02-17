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
public class MetaDataChangeRequest implements PathUpdates {

    @Id
    private String id;

    @NotNull
    private String metaDataId;

    @NotNull
    private String type;

    @NotNull
    private Map<String, Object> pathUpdates;

    @NotNull
    private Map<String, Object> auditData;

    private Map<String, Object> metaDataSummary;

    public MetaDataChangeRequest(String metaDataId, String type, Map<String, Object> pathUpdates, Map<String, Object> auditData) {
        this.metaDataId = metaDataId;
        this.type = type;
        this.pathUpdates = pathUpdates;
        this.auditData = auditData;
    }

    public void setMetaDataSummary(Map<String, Object> metaDataSummary) {
        this.metaDataSummary = metaDataSummary;
    }
}
