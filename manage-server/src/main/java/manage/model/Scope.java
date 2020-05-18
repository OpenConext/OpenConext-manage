package manage.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
@ToString
@Document(collection = "scopes")
public class Scope implements Serializable {

    @Id
    private String id;

    @Version
    private Long version;

    @NotNull
    @Indexed(unique = true)
    private String name;

    private Map<String, String> descriptions;

    public Scope(String name, Map<String, String> descriptions) {
        this.name = name;
        this.descriptions = descriptions;
    }
}
