package manage.mongo;

import com.github.mongobee.Mongobee;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import manage.conf.IndexConfiguration;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.IndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
@ChangeLog
@SuppressWarnings("unchecked")
public class MongobeeConfiguration {

    public static final String REVISION_POSTFIX = "_revision";

    private static final Logger LOG = LoggerFactory.getLogger(MongobeeConfiguration.class);

    private static MetaDataAutoConfiguration staticMetaDataAutoConfiguration;

    @Autowired
    private MetaDataAutoConfiguration metaDataAutoConfiguration;

    @Autowired
    private MappingMongoConverter mongoConverter;

    // Converts . into a mongo friendly char
    @PostConstruct
    public void setUpMongoEscapeCharacterConversion() {
        mongoConverter.setMapKeyDotReplacement("@");
    }

    @Bean
    public Mongobee mongobee(@Value("${spring.data.mongodb.uri}") String uri) {
        Mongobee runner = new Mongobee(uri);
        runner.setChangeLogsScanPackage("manage.mongo");
        MongobeeConfiguration.staticMetaDataAutoConfiguration = metaDataAutoConfiguration;
        return runner;
    }

    @ChangeSet(order = "001", id = "createCollections", author = "Okke Harsta")
    public void createCollections(MongoTemplate mongoTemplate) {
        Set<String> schemaNames = staticMetaDataAutoConfiguration.schemaNames();
        schemaNames.forEach(schema -> {
            if (!mongoTemplate.collectionExists(schema)) {
                mongoTemplate.createCollection(schema);
                staticMetaDataAutoConfiguration.indexConfigurations(schema).stream()
                        .map(this::indexDefinition)
                        .forEach(mongoTemplate.indexOps(schema)::ensureIndex);

                String revision = schema.concat(REVISION_POSTFIX);
                mongoTemplate.createCollection(revision);
                mongoTemplate.indexOps(revision).ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
            }
        });
        Arrays.asList("saml20_sp", "saml20_idp").forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.getIndexInfo().stream()
                    .filter(indexInfo -> indexInfo.getName().contains("data.eid"))
                    .forEach(indexInfo -> indexOps.dropIndex(indexInfo.getName()));
            indexOps.ensureIndex(new Index("data.eid", Sort.Direction.ASC).unique());
            if (indexOps.getIndexInfo().stream().anyMatch(indexInfo -> indexInfo.getName().equals("field_entityid"))) {
                indexOps.dropIndex("field_entityid");
            }
            indexOps.ensureIndex(new Index("data.entityid", Sort.Direction.ASC).unique());
            indexOps.ensureIndex(new Index("data.state", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("data.allowedall", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("data.allowedEntities.name", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("metaDataFields.coin:institution_id", Sort.Direction.ASC));
        });
        Arrays.asList("saml20_sp_revision", "saml20_idp_revision").forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
        });

        long max = Math.max(highestEid(mongoTemplate, "saml20_idp"), highestEid(mongoTemplate, "saml20_sp"));

        if (mongoTemplate.collectionExists("sequences")) {
            mongoTemplate.dropCollection("sequences");
        }
        mongoTemplate.createCollection("sequences");
        LOG.info("Creating sequence collection with new start seq {}", max + 1L);
        mongoTemplate.save(new Sequence("sequence", max + 1L));

    }

    @ChangeSet(order = "023", id = "migrateAttrMotivationMetaDataToArpAgainAgain", author = "Okke Harsta")
    public void migrateAttrMotivationMetaDataToArpAgain(MongoTemplate mongoTemplate) {
        doMigrateMotivation(mongoTemplate);
    }

    @ChangeSet(order = "024", id = "removeEmptyManipulations", author = "Okke Harsta")
    public void removeEmptyManipulation(MongoTemplate mongoTemplate) {
        Query query = new Query();
        query.addCriteria(Criteria.where("data.manipulation").regex("^\\s*$"));

        Arrays.asList(EntityType.values()).forEach(entityType -> {
            String collectionName = entityType.getType();
            List<Map> results = mongoTemplate.find(query, Map.class, collectionName);
            results.forEach(provider -> {
                Map data = Map.class.cast(provider.get("data"));
                String manipulation = (String) data.get("manipulation");
                if (manipulation != null && manipulation.replaceAll("\\s+", "").equals("")) {
                    data.put("manipulation", null);
                    mongoTemplate.save(provider, collectionName);
                    LOG.info("Nullified empty manipulation for {}", data.get("entityid"));
                }
            });
        });
    }

    protected void doMigrateMotivation(MongoTemplate mongoTemplate) {
        MappingMongoConverter converter = (MappingMongoConverter) mongoTemplate.getConverter();
        converter.setMapKeyDotReplacement("@");

        Query query = new Query();
        List<String> arpMotivations = Arrays.asList(
                "coin:attr_motivation:eduPersonEntitlement", "coin:attr_motivation:schacPersonalUniqueCode",
                "coin:attr_motivation:preferredLanguage", "coin:attr_motivation:mail",
                "coin:attr_motivation:eduPersonAffiliation", "coin:attr_motivation:displayName",
                "coin:attr_motivation:givenName", "coin:attr_motivation:schacHomeOrganizationType",
                "coin:attr_motivation:cn", "coin:attr_motivation:uid", "coin:attr_motivation:eduPersonScopedAffiliation",
                "coin:attr_motivation:eduPersonTargetedID", "coin:attr_motivation:schacHomeOrganization",
                "coin:attr_motivation:eduPersonOrcid", "coin:attr_motivation:isMemberOf",
                "coin:attr_motivation:eduPersonPrincipalName", "coin:attr_motivation:sn");

        Map<String, String> attrMotivationMap = arpMotivations.stream()
                .collect(Collectors.toMap(k -> k, k -> k.substring(k.lastIndexOf(":") + 1)));

        Map<String, String> arpAttributes = (Map<String, String>) Map.class.cast(Map.class.cast(Map.class.cast(Map
                .class.cast(Map.class.cast(staticMetaDataAutoConfiguration.schemaRepresentation(EntityType.SP)
                .get("properties")).get("arp")).get("properties")).get("attributes")).get("properties"))
                .keySet().stream().collect(Collectors.toMap(k -> {
                    String key = (String) k;
                    return key.substring(key.lastIndexOf(":") + 1);
                }, k -> k));

        Assert.isTrue(arpAttributes.keySet().containsAll(attrMotivationMap.values()), "Not all ");

        List<CriteriaDefinition> criteriaDefinitions = attrMotivationMap.keySet().stream()
                .map(key -> Criteria.where("data.metaDataFields.".concat(key)).exists(true))
                .collect(Collectors.toList());
        Criteria[] criteria = criteriaDefinitions.toArray(new Criteria[]{});
        query.addCriteria(new Criteria().orOperator(criteria));
        Arrays.asList(EntityType.SP.getType(), "single_tenant_template").forEach(type -> {
            List<MetaData> entities = mongoTemplate.find(query, MetaData.class, type);

            entities.forEach(entity -> {
                Map<String, Object> dmFields = (Map<String, Object>) entity.getData().get("metaDataFields");
                Map<String, Object> arp = (Map<String, Object>) entity.getData().get("arp");
                Map<String, Object> attributes = (Map<String, Object>) arp.get("attributes");
                attrMotivationMap.keySet().forEach(k -> {
                    String motivation = (String) dmFields.remove(k);
                    if (motivation != null) {
                        String arpSimpleAttribute = attrMotivationMap.get(k);
                        String arpAttribute = arpAttributes.get(arpSimpleAttribute);
                        List newArpAttribute = (List) attributes.getOrDefault(arpAttribute, new ArrayList<>());
                        if (newArpAttribute.isEmpty()) {
                            Map<String, String> replacementForDeletedMotivation = new HashMap<>();
                            replacementForDeletedMotivation.put("source", "idp");
                            replacementForDeletedMotivation.put("value", "*");
                            replacementForDeletedMotivation.put("motivation", motivation);
                            newArpAttribute.add(replacementForDeletedMotivation);
                            attributes.put(arpAttribute, newArpAttribute);
                        } else {
                            newArpAttribute.forEach(m -> {
                                Map<String, String> existingArpAttribute = (Map<String, String>) m;
                                existingArpAttribute.put("motivation", motivation);
                            });
                        }
                    }
                });
                LOG.info("Saving metadata {} with removed coin:attr_motivation metaData", entity.getId());
                mongoTemplate.save(entity, type);
            });
        });
    }


    private Long highestEid(MongoTemplate mongoTemplate, String type) {
        Query query = new Query().limit(1).with(new Sort(Sort.Direction.DESC, "data.eid"));
        query.fields().include("data.eid");
        Map res = mongoTemplate.findOne(query, Map.class, type);
        if (res == null) {
            return 10L;
        }
        return Long.valueOf(Map.class.cast(res.get("data")).get("eid").toString());
    }

    private IndexDefinition indexDefinition(IndexConfiguration indexConfiguration) {
        Index index = new Index();
        indexConfiguration.getFields().forEach(field -> index.on("data.".concat(field), Sort.Direction.ASC));
        index.named(indexConfiguration.getName());
        if (indexConfiguration.isUnique()) {
            index.unique();
        }
        return index;
    }

}
