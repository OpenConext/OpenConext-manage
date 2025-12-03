package manage.service.jobs;

import lombok.SneakyThrows;
import manage.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ClusterLockServiceTest extends AbstractIntegrationTest {

    @SneakyThrows
    @Test
    void tryAcquire() {
        MongoTemplate mongoTemplate = metaDataRepository.getMongoTemplate();
        mongoTemplate.remove(new Query(), "cluster_locks");

        ClusterLockService lockService = new ClusterLockService(mongoTemplate);
        String lockName = "lock-1";
        boolean isLockAcquired = lockService.tryAcquire(lockName, 60 * 5);
        assertTrue(isLockAcquired);

        boolean isLockAcquiredAgain = lockService.tryAcquire(lockName, 60 * 5);
        assertFalse(isLockAcquiredAgain);

        lockService.release(lockName);
        List<ClusterLock> clusterLocks = mongoTemplate.findAll(ClusterLock.class, "cluster_locks");
        assertEquals(0, clusterLocks.size());

        boolean reentrantLockAcquired = lockService.tryAcquire(lockName, 0);
        assertTrue(reentrantLockAcquired);
    }

}
