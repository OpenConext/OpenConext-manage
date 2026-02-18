package manage.model;

import lombok.*;
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

    @Setter
    private boolean incrementalChange;

    @Setter
    private PathUpdateType pathUpdateType;

    @NotNull
    private Map<String, Object> pathUpdates;

    @NotNull
    private Map<String, Object> auditData;

    @Setter
    private Instant created;

    @Setter
    private Map<String, Object> metaDataSummary;

    @Setter
    private String requestType;

    @Setter
    private String ticketKey;

    public MetaDataChangeRequest(String metaDataId, String type, String note, Map<String, Object> pathUpdates, Map<String, Object> auditData) {
        this.metaDataId = metaDataId;
        this.type = type;
        this.note = note;
        this.pathUpdates = pathUpdates;
        this.auditData = auditData;
        this.created = Instant.now();
    }

    @Override
    public Map<String, Object> getExternalReferenceData() {
        return Collections.emptyMap();
    }
}
