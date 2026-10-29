package manage.mongo.changeunit;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import manage.model.EntityType;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

@SuppressWarnings({"unchecked", "deprecation"})
@ChangeUnit(id = "addCreatedOnAndPublishedOn", order = "020", author = "okke.harsta@surf.nl", transactional = false)
public class AddCreatedOnAndPublishedOn {

    private static final Logger LOG = LoggerFactory.getLogger(AddCreatedOnAndPublishedOn.class);

    @Execution
    public void addCreatedOnAndPublishedOn(MongoTemplate mongoTemplate) {
        Stream.of(EntityType.values()).map(EntityType::getType).forEach(type -> {
            if (!mongoTemplate.collectionExists(type)) {
                return;
            }
            String revisionCollection = type.concat(REVISION_POSTFIX);
            boolean hasRevisions = mongoTemplate.collectionExists(revisionCollection);
            AtomicInteger count = new AtomicInteger();
            mongoTemplate.getCollection(type).find().forEach(live -> {
                List<Document> versions = new ArrayList<>();
                if (hasRevisions) {
                    mongoTemplate.getCollection(revisionCollection)
                        .find(new Document("revision.parentId", live.get("_id")))
                        .sort(new Document("revision.number", 1))
                        .forEach(versions::add);
                }
                //The live version is the last one in the chain
                versions.add(live);

                Date createdOn = revisionCreated(versions.get(0));
                if (createdOn == null) {
                    createdOn = revisionCreated(live);
                }
                //Entities created straight as prodaccepted are published on creation
                Date publishedOn = "prodaccepted".equals(state(versions.get(0))) ? createdOn : null;
                for (int i = 1; i < versions.size() && publishedOn == null; i++) {
                    if ("testaccepted".equals(state(versions.get(i - 1))) && "prodaccepted".equals(state(versions.get(i)))) {
                        publishedOn = revisionCreated(versions.get(i));
                    }
                }
                Update update = new Update();
                if (createdOn != null) {
                    update.set("created_on", createdOn);
                }
                if (publishedOn != null) {
                    update.set("published_on", publishedOn);
                }
                if (!update.getUpdateObject().isEmpty()) {
                    mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(live.get("_id"))), update, type);
                    count.incrementAndGet();
                }
            });
            LOG.info("Added created_on and published_on to {} documents in {}", count.get(), type);
        });
    }

    @RollbackExecution
    public void rollback() {
        // Migrations are not rolled back
    }

    private Date revisionCreated(Document document) {
        Document revision = document.get("revision", Document.class);
        return revision != null ? revision.getDate("created") : null;
    }

    private String state(Document document) {
        Document data = document.get("data", Document.class);
        return data != null ? data.getString("state") : null;
    }
}
