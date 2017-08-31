package mr.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import mr.mongo.MongobeeConfiguration;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.util.Assert;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static mr.mongo.MongobeeConfiguration.REVISION_POSTFIX;

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

    public void initial(String id, String createdBy) {
        this.id = id;
        this.revision = new Revision(0, Instant.now(), null, createdBy);
    }

    public void revision(String newId) {
        this.type = this.type.concat(REVISION_POSTFIX);
        this.revision.setParentId(this.id);
        this.id = newId;
    }

    public void promoteToLatest(String updatedBy) {
        this.revision = new Revision(revision.getNumber() + 1, Instant.now(), null, updatedBy);
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    public void merge(MetaDataUpdate metaDataUpdate) {
        metaDataUpdate.getPathUpdates().forEach((path, value) -> {
            List<String> parts = Arrays.asList(path.split("\\."));
            Iterator<String> iterator = parts.iterator();
            String part = iterator.next();
            Object reference = parts.size() == 1 ? this.data : this.data.get(part);
            String property = path;
            while (iterator.hasNext()) {
                Assert.notNull(reference, String.format("Invalid metadata path %s. %s part does not exists", value, part));
                part = iterator.next();
                if (iterator.hasNext()) {
                    reference = Map.class.cast(reference).get(part);
                } else {
                    property = part;
                }
            }
            Map.class.cast(reference).put(property, value);
        });
    }
}
