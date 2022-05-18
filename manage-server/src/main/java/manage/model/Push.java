package manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "pushes")
public class Push {

    @NotNull
    private String id;

    @NotNull
    private Instant created;

    @NotNull
    private String userId;

}
