package manage.repository;

import manage.model.EntityType;
import manage.model.MetaData;
import manage.mongo.Sequence;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * We can't use the Spring JPA repositories as we at runtime need to decide which collection to use. We only have one
 * Document type - e.g. MetaData - and more then one MetaData collections.
 */
@Repository
public class MetaDataRepository {

    private static final int AUTOCOMPLETE_LIMIT = 16;

    private MongoTemplate mongoTemplate;

    @Autowired
    public MetaDataRepository(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    public MetaData findById(String id, String type) {
        return mongoTemplate.findById(id, MetaData.class, type);
    }

    public MetaData save(MetaData metaData) {
        mongoTemplate.insert(metaData, metaData.getType());
        return metaData;
    }

    public void remove(MetaData metaData) {
        mongoTemplate.remove(metaData, metaData.getType());
    }

    public List<MetaData> revisions(String type, String parentId) {
        Query query = new Query(Criteria.where("revision.parentId").is(parentId));
        return mongoTemplate.find(query, MetaData.class, type);
    }

    public void update(MetaData metaData) {
        mongoTemplate.save(metaData, metaData.getType());
    }

    public MongoTemplate getMongoTemplate() {
        return mongoTemplate;
    }

    public List<Map> autoComplete(String type, String search) {
        Query query = queryWithSamlFields();
        if ("*".equals(search)) {
            return mongoTemplate.find(query, Map.class, type);
        }
        search = escapeSpecialChars(search);
        query.limit(AUTOCOMPLETE_LIMIT);
        Criteria criteria = new Criteria();
        query.addCriteria(criteria.orOperator(
            regex("data.entityid", search),
            regex("data.metaDataFields.name:en", search),
            regex("data.metaDataFields.name:nl", search),
            regex("data.metaDataFields.keywords:en", search),
            regex("data.metaDataFields.keywords:nl", search)));
        return mongoTemplate.find(query, Map.class, type);
    }

    public List<Map> allServiceProviderEntityIds() {
        Query query = new Query();
        query
            .fields()
            .include("data.entityid")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.coin:imported_from_edugain")
            .include("data.metaDataFields.coin:publish_in_edugain");
        return mongoTemplate.find(query, Map.class, EntityType.SP.getType());
    }

    public int deleteAllImportedServiceProviders() {
        Query query = new Query(Criteria.where("data.metaDataFields.coin:imported_from_edugain").is("1"));
        return mongoTemplate.remove(query, EntityType.SP.getType()).getN();
    }

    public long countAllImportedServiceProviders() {
        Query query = new Query(Criteria.where("data.metaDataFields.coin:imported_from_edugain").is("1"));
        return mongoTemplate.count(query, EntityType.SP.getType());
    }


    protected String escapeSpecialChars(String query) {
        return query.replaceAll("([\\Q\\/$^.?*+{}()|[]\\E])", "\\\\$1");
    }

    private Criteria regex(String key, String search) {
        try {
            return Criteria.where(key).regex(".*" + search + ".*", "i");
        } catch (IllegalArgumentException e) {
            //ignore
            return Criteria.where("nope").exists(true);
        }
    }

    private String escapeMetaDataField(String key) {
        if (key.startsWith("metaDataFields")) {
            return "metaDataFields." + key.substring("metaDataFields.".length())
                .replaceAll("\\.", "@");
        }
        if (key.startsWith("arp.attributes")) {
            return "arp.attributes." + key.substring("arp.attributes.".length())
                .replaceAll("\\.", "@");
        }
        return key;

    }

    public List<Map> search(String type, Map<String, Object> properties, List<String> requestedAttributes, Boolean
        allAttributes, Boolean logicalOperatorIsAnd) {
        Query query = allAttributes ? new Query() : queryWithSamlFields();
        if (!allAttributes) {
            requestedAttributes.forEach(requestedAttribute -> {
                String key = escapeMetaDataField(requestedAttribute);
                query.fields().include("data.".concat(key));
            });
            if (type.contains("revision")) {
                query.fields().include("revision").include("data.revisionnote");
            }
        }
        List<CriteriaDefinition> criteriaDefinitions = new ArrayList<>();

        properties.forEach((key, value) -> {
            key = escapeMetaDataField(key);

            if (value instanceof Boolean && Boolean.class.cast(value) && key.contains("attributes")) {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).exists(true));
            } else if (value instanceof String && !StringUtils.hasText(String.class.cast(value))) {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).exists(false));
            } else if ("*".equals(value)) {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).regex(".*", "i"));
            } else if (value instanceof String && String.class.cast(value).contains("*")) {
                String queryString = String.class.cast(value);
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).regex(queryString, "i"));
            } else if (value instanceof List && !List.class.cast(value).isEmpty()) {
                List l = List.class.cast(value);
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).in(l));
            } else {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).is(value));
            }
        });
        if (criteriaDefinitions.isEmpty()) {
            criteriaDefinitions.add(Criteria.where("data").exists(true));
        }
        Criteria[] criteria = criteriaDefinitions.toArray(new Criteria[]{});
        if (logicalOperatorIsAnd) {
            query.addCriteria(new Criteria().andOperator(criteria));
        } else {
            query.addCriteria(new Criteria().orOperator(criteria));
        }
        return mongoTemplate.find(query, Map.class, type);
    }

    public List<MetaData> findRaw(String type, String query) {
        return mongoTemplate.find(new BasicQuery(query), MetaData.class, type);
    }

    public List<Map> whiteListing(String type) {
        Query query = queryWithSamlFields();
        query.fields()
            .include("data.allowedall")
            .include("data.allowedEntities");
        return mongoTemplate.find(query, Map.class, type);
    }

    public Long incrementEid() {
        Update updateInc = new Update();
        updateInc.inc("value", 1L);
        Sequence res = mongoTemplate.findAndModify(new BasicQuery("{\"_id\":\"sequence\"}"), updateInc, Sequence.class);
        return res.getValue();
    }

    private Query queryWithSamlFields() {
        Query query = new Query();
        //When we have multiple types then we need to delegate depending on the type.
        query
            .fields()
            .include("version")
            .include("data.state")
            .include("data.entityid")
            .include("data.notes")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.name:nl");
        return query;
    }
}
