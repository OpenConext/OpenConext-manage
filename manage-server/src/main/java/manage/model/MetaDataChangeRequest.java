package manage.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Collections;
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

    private String note;

    private boolean incrementalChange;

    private PathUpdateType pathUpdateType;

    @NotNull
    private Map<String, Object> pathUpdates;

    @NotNull
    private Map<String, Object> auditData;

    private Instant created;

    private Map<String, Object> metaDataSummary;

    public MetaDataChangeRequest(String metaDataId, String type, String note, Map<String, Object> pathUpdates, Map<String, Object> auditData) {
        this.metaDataId = metaDataId;
        this.type = type;
        this.note = note;
        this.pathUpdates = pathUpdates;
        this.auditData = auditData;
        this.created = Instant.now();
    }

    public void setMetaDataSummary(Map<String, Object> metaDataSummary) {
        this.metaDataSummary = metaDataSummary;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public void setIncrementalChange(boolean incrementalChange) {
        this.incrementalChange = incrementalChange;
    }

    public void setPathUpdateType(PathUpdateType pathUpdateType) {
        this.pathUpdateType = pathUpdateType;
    }

    @Override
    public Map<String, Object> getExternalReferenceData() {
        return Collections.emptyMap();
    }
}
