package manage.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class MetaData implements Serializable {

    @Id
    private String id;

    @Version
    private Long version;

    @NotNull
    private String type;

    private Revision revision;

    @NotNull
    private Map<String, Object> data;

    public MetaData(String type, Map<String, Object> data) {
        this.type = type;
        this.data = data;
    }

    public void initial(String id, String createdBy, Long eid) {
        this.id = id;
        this.revision = new Revision(0, Instant.now(), null, createdBy);
        this.data.put("eid", eid);
    }

    public void revision(String newId) {
        this.type = this.type.contains(REVISION_POSTFIX) ? this.type : this.type.concat(REVISION_POSTFIX);
        this.getNonNullRevision().setParentId(this.id);
        this.id = newId;
    }

    public void terminate(String newId, String revisionNote) {
        this.id = newId;
        this.getData().put("revisionnote", revisionNote);
        this.revision.terminate();
    }

    private Revision getNonNullRevision() {
        if (this.revision == null) {
            //can only happen when MetaData is inserted not by API code, but by scripts like testing
            this.revision = new Revision(0, Instant.now(), null, "system");
        }
        return this.revision;
    }

    public void promoteToLatest(String updatedBy, String revisionNote) {
        this.revision = new Revision(this.getNonNullRevision().getNumber() + 1, Instant.now(), null, updatedBy);
        this.getData().put("revisionnote", revisionNote);
    }

    public void restoreToLatest(String newId, Long version, String updatedBy, int latestRevisionNumber, String newType) {
        this.type = newType;
        this.id = newId;
        this.version = version;
        this.getData().put("revisionnote", "Restore of revision: " + getNonNullRevision().getNumber());
        this.revision = new Revision(latestRevisionNumber + 1, Instant.now(), null, updatedBy);
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @SuppressWarnings("unchecked")
    public void merge(MetaDataUpdate metaDataUpdate) {
        metaDataUpdate.getPathUpdates().forEach((path, value) -> {
            List<String> parts = Arrays.asList(path.split("\\."));
            Iterator<String> iterator = parts.iterator();
            String part = iterator.next();
            Object reference = parts.size() == 1 ? this.data : this.data.get(part);
            String property = path;
            while (iterator.hasNext()) {
                Assert.notNull(reference, String.format("Invalid metadata path %s. %s part does not exists", value,
                        part));
                part = iterator.next();
                if (iterator.hasNext()) {
                    reference = ((Map) reference).get(part);
                } else {
                    property = part;
                }
            }
            ((Map) reference).put(property, value);
        });
    }

    @Transient
    public Map<String, Object> metaDataFields() {
        return (Map) data.get("metaDataFields");
    }
}
