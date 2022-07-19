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
import java.util.*;
import java.util.stream.Collectors;

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

    public void terminate(String newId, String revisionNote, String uid) {
        this.id = newId;
        this.getData().put("revisionnote", revisionNote);
        this.revision.terminate(uid);
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

    public void setType(String type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public void merge(PathUpdates pathUpdates) {
        pathUpdates.getPathUpdates().forEach((path, value) -> {
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
            if (pathUpdates.isIncrementalChange()) {
                List<Map<String, Object>> referenceValue = (List<Map<String, Object>>) ((Map) reference).get(property);
                if (PathUpdateType.ADDITION.equals(pathUpdates.getPathUpdateType())) {
                    if (referenceValue == null) {
                        referenceValue = new ArrayList<>();
                    } else {
                        //we need top copy in case the referenceValue is immutable
                        referenceValue = new ArrayList<>(referenceValue);
                    }
                    //In case for example a Loa, the level could be updated. So first remove any existing one's, but
                    ((Map) reference).put(property, referenceValue);
                    if (value instanceof Map) {
                        referenceValue.removeIf((ref -> ref.get("name").equals(((Map) value).get("name"))));
                        referenceValue.add((Map) value);
                    } else if (value instanceof List) {
                        referenceValue.removeIf(ref -> ((List) value).stream().anyMatch(m -> ref.get("name").equals(((Map) m).get("name"))));
                        referenceValue.addAll((List) value);
                    }
                } else if (referenceValue != null) {
                    if (value instanceof Map) {
                        referenceValue.removeIf(m -> m.get("name").equals(((Map) value).get("name")));
                    } else if (value instanceof List) {
                        List<Map<String, Object>> valueList = (List<Map<String, Object>>) value;
                        referenceValue.removeIf(m -> valueList.stream().anyMatch(ms -> ms.get("name").equals(m.get("name"))));
                    }
                }
            } else {
                if (value == null) {
                    ((Map) reference).remove(property);
                } else {
                    ((Map) reference).put(property, value);
                }
            }
        });
    }

    @Transient
    public Map<String, Object> metaDataFields() {
        return (Map) data.get("metaDataFields");
    }

    @Transient
    public Map<String, Object> summary() {
        Map<String, Object> results = new HashMap<>();
        results.put("entityid", data.get("entityid"));
        results.put("state", data.get("state"));

        Map<String, Object> metaDataFields = metaDataFields();
        results.put("name", metaDataFields.get("name:en"));
        results.put("organizationName", metaDataFields.get("OrganizationName:en"));

        return results;
    }

    @SuppressWarnings("unchecked")
    private Object doTrimSpaces(Object value) {
        if (value instanceof String) {
            return ((String) value).trim();
        }
        if (value instanceof Map) {
            try {
                ((Map) value).replaceAll((key, val) -> doTrimSpaces(val));
                return value;
            } catch (UnsupportedOperationException e) {
                //Collections.singletonMap does not support replaceAll
                return value;
            }
        }
        if (value instanceof List) {
            try {
                return ((List) value).stream().map(o -> doTrimSpaces(o)).collect(Collectors.toList());
            } catch (UnsupportedOperationException e) {
                //Collections.singletonList does not support replaceAll
                return value;
            }
        }
        return value;
    }

    public void trimSpaces() {
        data.replaceAll((key, value) -> doTrimSpaces(value));
    }
}
