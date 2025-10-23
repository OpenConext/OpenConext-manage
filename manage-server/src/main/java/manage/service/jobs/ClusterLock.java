package manage.service.jobs;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Document("cluster_locks")
public class ClusterLock {

    @Id
    private String name;

    private String ownerId;

    @Indexed(expireAfter = "0s")
    private Instant expiresAt;

}
