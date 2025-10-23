package manage.service.jobs;

import com.mongodb.DuplicateKeyException;
import lombok.Getter;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class ClusterLockService {

    private final MongoTemplate mongoTemplate;
    @Getter
    private final String nodeId = UUID.randomUUID().toString(); // identify this node

    public ClusterLockService(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * Try to acquire a named lock for a limited time.
     */
    public boolean tryAcquire(String lockName, int ttlSeconds) {
        Instant expiresAt = Instant.now().plus(ttlSeconds, ChronoUnit.SECONDS);
        ClusterLock lock = new ClusterLock(lockName, nodeId, expiresAt);
        try {
            mongoTemplate.insert(lock);
            return true;
        } catch (DuplicateKeyException e) {
            return false;
        }
    }

    /**
     * Release the lock if this node owns it.
     */
    public void release(String lockName) {
        mongoTemplate.remove(
            mongoTemplate.findById(lockName, ClusterLock.class)
        );
    }

}
