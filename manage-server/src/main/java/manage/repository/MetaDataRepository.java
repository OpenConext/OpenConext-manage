package manage.repository;

import lombok.Getter;
import manage.exception.ValueNotUniqueException;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.MetaDataChangeRequest;
import manage.model.StatsEntry;
import manage.mongo.Sequence;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static manage.mongo.MongoChangelog.CHANGE_REQUEST_POSTFIX;
import static manage.mongo.MongoChangelog.REVISION_POSTFIX;

/**
 * We can't use the Spring JPA repositories as we at runtime need to decide which collection to use. We only have one
 * Document type - e.g. MetaData - and more than one MetaData collections.
 */
@Repository
public class MetaDataRepository {

    private static final int AUTOCOMPLETE_LIMIT = 16;

    private static final String FIELD_NOT_UNIQUE_MESSAGE = "A %s with the value %s for field %s already exists.";

    @Getter
    private final MongoTemplate mongoTemplate;
    private final List<String> supportedLanguages;

    private final FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true);

    @Autowired
    public MetaDataRepository(MongoTemplate mongoTemplate,
                              @Value("${product.supported_languages}") String supportedLanguages) {
        this.mongoTemplate = mongoTemplate;
        this.supportedLanguages = Stream.of(supportedLanguages.split(",")).map(String::trim).collect(toList());
    }

    public MetaData findById(String id, String type) {
        return mongoTemplate.findById(id, MetaData.class, type);
    }

    public List<MetaData> findAllByType(String type) {
        return mongoTemplate.findAll(MetaData.class, type);
    }

    public List<MetaData> retrieveAllEntities() {
        return Stream.concat(
            mongoTemplate.findAll(MetaData.class, EntityType.SP.getType()).stream(),
            mongoTemplate.findAll(MetaData.class, EntityType.RP.getType()).stream()).toList();
    }

    public MetaData save(MetaData metaData) {
        metaData.trimSpaces();
        mongoTemplate.insert(metaData, metaData.getType());
        return metaData;
    }

    public MetaDataChangeRequest save(MetaDataChangeRequest metaDataChangeRequest) {
        return mongoTemplate.insert(metaDataChangeRequest, metaDataChangeRequest.getType().concat(CHANGE_REQUEST_POSTFIX));
    }

    public void remove(MetaData metaData) {
        mongoTemplate.remove(metaData, metaData.getType());
    }

    public List<MetaData> revisions(String type, String parentId) {
        Query query = new Query(Criteria.where("revision.parentId").is(parentId));
        return mongoTemplate.find(query, MetaData.class, type);
    }

    public List<MetaDataChangeRequest> changeRequests(String metaDataId, String collectionName) {
        Query query = new Query(Criteria.where("metaDataId").is(metaDataId));
        return mongoTemplate.find(query, MetaDataChangeRequest.class, collectionName);
    }

    public List<MetaDataChangeRequest> allChangeRequests() {
        List<MetaDataChangeRequest> results = new ArrayList<>();
        Stream.of(EntityType.values()).forEach(entityType -> {
            results.addAll(mongoTemplate.findAll(MetaDataChangeRequest.class, entityType.getType().concat(CHANGE_REQUEST_POSTFIX)));
        });
        return results;
    }

    public long openChangeRequests() {
        AtomicLong count = new AtomicLong(0);
        Query query = new Query();
        Stream.of(EntityType.values()).forEach(entityType -> {
            count.addAndGet(mongoTemplate.count(query, entityType.getType().concat(CHANGE_REQUEST_POSTFIX)));
        });
        return count.get();
    }

    public void update(MetaData metaData) {
        metaData.trimSpaces();
        mongoTemplate.save(metaData, metaData.getType());
    }

    public List<Map> autoComplete(String type, String search) {
        Query query = queryWithSamlFields(EntityType.fromType(type));
        if ("*".equals(search)) {
            return mongoTemplate.find(query, Map.class, type);
        }
        String escapedSearch = escapeSpecialChars(search);
        query.limit(AUTOCOMPLETE_LIMIT);
        Criteria criteria = new Criteria();

        List<String> parts = Arrays.asList(escapedSearch.split(" "));
        criteria.andOperator(parts.stream().map(part -> {
            List<Criteria> orCriterias = new ArrayList<>();
            orCriterias.add(regex("data.entityid", part));
            if (EntityType.PDP.getType().equals(type)) {
                orCriterias.add(regex("data.name", part));
                orCriterias.add(regex("data.description", part));
            } else if (EntityType.ORG.getType().equals(type)) {
                orCriterias.add(regex("data.name", part));
                orCriterias.add(regex("data.kvkNumber", part));
                orCriterias.add(regex("data.notes", part));
            } else {
                this.supportedLanguages.forEach(lang -> {
                    orCriterias.add(regex("data.metaDataFields.name:" + lang, part));
                    orCriterias.add(regex("data.metaDataFields.displayName:" + lang, part));
                    orCriterias.add(regex("data.metaDataFields.keywords:0:" + lang, part));
                    orCriterias.add(regex("data.metaDataFields.OrganizationName:" + lang, part));
                });
            }
            return new Criteria().orOperator(orCriterias.toArray(new Criteria[orCriterias.size()]));
        }).toArray(Criteria[]::new));
        query.addCriteria(criteria);
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

    public long deleteAllImportedServiceProviders() {
        Query query = new Query(Criteria.where("data.metaDataFields.coin:imported_from_edugain").is(true));
        return mongoTemplate.remove(query, EntityType.SP.getType()).getDeletedCount();
    }

    public long countAllImportedServiceProviders() {
        Query query = new Query(Criteria.where("data.metaDataFields.coin:imported_from_edugain").is(true));
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
        Query query = allAttributes ? new Query() : queryWithSamlFields(EntityType.fromType(type.replaceAll("_revision", "")));
        if (!allAttributes) {
            requestedAttributes.forEach(requestedAttribute -> {
                String key = escapeMetaDataField(requestedAttribute);
                query.fields().include("data.".concat(key));
                query.fields().include("revision.created");
            });
            if (type.contains("revision")) {
                query.fields().include("revision").include("data.revisionnote");
            }
        }
        List<CriteriaDefinition> criteriaDefinitions = new ArrayList<>();

        properties.forEach((key, value) -> {
            key = escapeMetaDataField(key);

            if (value instanceof Boolean && (Boolean) value && key.contains("attributes")) {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).exists(true));
            } else if (value instanceof String && !StringUtils.hasText((String) value)) {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).exists(false));
            } else if (value instanceof String && StringUtils.hasText((String) value) &&
                ("true".equalsIgnoreCase((String) value) || "false".equalsIgnoreCase((String) value))) {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).is(Boolean.parseBoolean((String) value)));
            } else if (value instanceof String && StringUtils.hasText((String) value) && isNumeric((String) value)) {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).is(Integer.parseInt((String) value)));
            } else if ("*".equals(value)) {
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).regex(".*", "i"));
            } else if (value instanceof String && ((String) value).contains("*")) {
                String queryString = (String) value;
                criteriaDefinitions.add(Criteria.where("data.".concat(key)).regex(queryString, "i"));
            } else if (value instanceof List && !((List) value).isEmpty()) {
                List l = (List) value;
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

    private boolean isNumeric(String value) {
        return value.matches("\\d+");
    }

    public List<MetaData> findRaw(String type, String query) {
        return mongoTemplate.find(new BasicQuery(query), MetaData.class, type);
    }

    public List<Map> findByEntityId(String type, String entityId) {
        Document document = new Document("data.entityid", entityId);
        Query query = new BasicQuery(document).collation(Collation.of("en").strength(2));
        query.fields().include("_id");
        return mongoTemplate.find(query, Map.class, type);
    }

    public List<MetaData> recentActivity(List<EntityType> types, int max) {
        max = Math.min(max, 100);
        Query query = new Query()
            .with(Sort.by(Sort.Order.desc("revision.created")))
            .limit(max);
        Field fields = query.fields();
        fields
            .include("type")
            .include("version")
            .include("data.state")
            .include("data.entityid")
            .include("data.name")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.OrganizationName:en")
            .include("data.metaDataFields.OrganizationName:en")
            .include("data.revisionnote")
            .include("revision.number")
            .include("revision.created")
            .include("revision.terminated")
            .include("revision.updatedBy");

        List<MetaData> metaData = types.stream()
            .map(entityType -> mongoTemplate.find(query, MetaData.class, entityType.getType()))
            .flatMap(List::stream)
            .collect(toList());
        List<MetaData> results = metaData.stream()
            .sorted(Comparator.comparing(md -> md.getRevision().getCreated(), Comparator.reverseOrder()))
            .collect(toList()).subList(0, Math.min(max, metaData.size()));

        Instant firstActivity = results.get(results.size() - 1).getRevision().getCreated();
        query.addCriteria(Criteria.where("revision.terminated").gte(firstActivity));
        List<MetaData> revisionMetaData = types.stream()
            .map(entityType -> mongoTemplate.find(query, MetaData.class, entityType.getType().concat(REVISION_POSTFIX)))
            .flatMap(List::stream)
            .map(this::updateCreatedRevision)
            .collect(toList());
        results.addAll(revisionMetaData);
        if (!revisionMetaData.isEmpty()) {
            //need to re-sort and cut off again
            results = results.stream()
                .sorted(Comparator.comparing(md -> md.getRevision().getCreated(), Comparator.reverseOrder()))
                .collect(toList()).subList(0, Math.min(max, metaData.size()));
        }

        return results;
    }

    private MetaData updateCreatedRevision(MetaData metaData) {
        metaData.getRevision().markCreatedWithTerminatedInstant();
        return metaData;
    }

    public List<Map> whiteListing(String type, String state) {
        Query query = queryWithSamlFields(EntityType.fromType(type)).addCriteria(Criteria.where("data.state").is(state));
        query.fields()
            .include("data.allowedall")
            .include("data.allowedEntities")
            .include("data.metaDataFields.coin:stepup:requireloa");
        List<Map> metaData = mongoTemplate.find(query, Map.class, type);
        if (type.equals(EntityType.SP.getType())) {
            List<Map> oidcMetaData = mongoTemplate.find(query, Map.class, EntityType.RP.getType());
            metaData.addAll(oidcMetaData);
        }
        return metaData;
    }

    public void validateFieldUnique(String type, String fieldName, Object value) {
        if (findAllByType(type).stream().anyMatch(m -> m.getData().get(fieldName).equals(value))) {
            throw new ValueNotUniqueException(String.format(FIELD_NOT_UNIQUE_MESSAGE, type, value, fieldName));
        }
    }

    public List<Map> relyingParties(String resourceServerEntityID) {
        Query query = queryWithSamlFields(EntityType.RP)
            .addCriteria(Criteria.where("data.allowedResourceServers.name").is(resourceServerEntityID));
        return mongoTemplate.find(query, Map.class, EntityType.RP.getType());
    }

    public List<Map> spPolicies(String entityID) {
        Query query = queryWithSamlFields(EntityType.PDP)
            .addCriteria(Criteria.where("data.serviceProviderIds.name").is(entityID));
        return mongoTemplate.find(query, Map.class, EntityType.PDP.getType());
    }

    public List<Map> idpPolicies(String entityID) {
        Query query = queryWithSamlFields(EntityType.PDP)
            .addCriteria(Criteria.where("data.identityProviderIds.name").is(entityID));
        return mongoTemplate.find(query, Map.class, EntityType.PDP.getType());
    }

    public List<Map> allowedEntities(String id, EntityType entityType) {
        Map byId = mongoTemplate.findById(id, Map.class, entityType.getType());
        Query query = queryWithSamlFields(entityType)
            .addCriteria(new Criteria().orOperator(
                Criteria.where("data.allowedEntities.name").is(((Map) byId.get("data")).get("entityid")),
                Criteria.where("data.allowedall").is(true)
            ));
        return mongoTemplate.find(query, Map.class, EntityType.IDP.getType());
    }

    public List<Map> provisioning(List<String> identifiers) {
        Query query = new Query()
            .addCriteria(Criteria.where("data.applications.id").in(identifiers));
        return mongoTemplate.find(query, Map.class, EntityType.PROV.getType());
    }

    public Map<String, List<MetaData>> conflictingPolicies() {
        Map<String, List<MetaData>> conflicts = new HashMap<>();
        List<MetaData> policies = this.mongoTemplate.findAll(MetaData.class, EntityType.PDP.getType());
        policies.forEach(policy -> {
            List.of("serviceProviderIds", "identityProviderIds").forEach(key -> {
                List<Map<String, String>> providers = (List<Map<String, String>>) policy.getData().getOrDefault(key, List.of());
                providers.forEach(provider -> {
                    String entityID = provider.get("name");
                    List<MetaData> metaDataList = conflicts.getOrDefault(entityID, new ArrayList<>());
                    metaDataList.add(policy);
                    conflicts.put(entityID, metaDataList);
                });
            });
        });
        return conflicts.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(entry -> entry.getKey(), entry -> entry.getValue()));
    }

    public synchronized Long incrementEid() {
        Update updateInc = new Update();
        updateInc.inc("value", 1L);
        Sequence res = mongoTemplate.findAndModify(new BasicQuery("{\"_id\":\"sequence\"}"), updateInc, options, Sequence.class);
        return res.getValue();
    }

    public List<StatsEntry> stats() {
        return mongoTemplate.getCollectionNames().stream()
            .filter(name -> !name.toLowerCase().contains("system"))
            .map(name -> new StatsEntry(name, mongoTemplate.count(new Query(), name)))
            .collect(toList());
    }

    private Query queryWithSamlFields(EntityType entityType) {
        Query query = new Query();
        //When we have multiple types then we need to delegate depending on the type.
        Field fields = query.fields();
        fields
            .include("version", "type", "data.state", "data.entityid", "data.notes");
        if (entityType.equals(EntityType.PDP)) {
            fields.include("data.name",
                "data.description",
                "data.type",
                "data.serviceProviderIds",
                "data.identityProviderIds");
        } else if (entityType.equals(EntityType.ORG)) {
            fields.include(
                "data.name",
                "data.kvkNumber",
                "data.notes");
        } else {
            this.supportedLanguages.forEach(lang -> {
                fields.include("data.metaDataFields.name:" + lang);
                fields.include("data.metaDataFields.OrganizationName:" + lang);
            });
        }
        return query;
    }

}
