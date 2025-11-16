package manage.mongo;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate;
import com.mongodb.bulk.BulkWriteResult;
import com.mongodb.client.DistinctIterable;
import lombok.SneakyThrows;
import manage.conf.MetaDataAutoConfiguration;
import manage.model.EntityType;
import manage.model.MetaData;
import manage.model.Scope;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.BulkOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.TextIndexDefinition;
import org.springframework.data.mongodb.core.query.Collation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ChangeLog(order = "001")
@SuppressWarnings("unchecked")
public class MongoChangelog {

    public static final String REVISION_POSTFIX = "_revision";
    public static final String CHANGE_REQUEST_POSTFIX = "_change_request";

    private static final Logger LOG = LoggerFactory.getLogger(MongoChangelog.class);

    @ChangeSet(order = "001", id = "createCollections", author = "okke.harsta@surf.nl")
    public void createCollections(MongockTemplate mongoTemplate) {
        this.doCreateSchemas(mongoTemplate, Arrays.asList("saml20_sp", "saml20_idp", "oidc10_rp"));
        if (!mongoTemplate.collectionExists("sequences")) {
            LOG.info("Creating sequence collection with new start seq {}", 999L);

            mongoTemplate.createCollection("sequences");
            mongoTemplate.save(new Sequence("sequence", 999L));
        }

    }

    @ChangeSet(order = "002", id = "addTextIndexes", author = "okke.harsta@surf.nl")
    public void addTextIndexes(MongockTemplate mongoTemplate) {
        doAddTestIndexes(mongoTemplate);
    }

    private void doAddTestIndexes(MongockTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            TextIndexDefinition textIndexDefinition = new TextIndexDefinition.TextIndexDefinitionBuilder()
                .onField("$**")
                .build();
            mongoTemplate.indexOps(entityType.getType()).ensureIndex(textIndexDefinition);
        });
    }

    @ChangeSet(order = "003", id = "addDefaultScopes", author = "okke.harsta@surf.nl")
    public void addDefaultScopes(MongockTemplate mongoTemplate) {
        if (!mongoTemplate.collectionExists("scopes")) {
            mongoTemplate.remove(new Query(), Scope.class);
            DistinctIterable<String> scopes = mongoTemplate.getCollection(EntityType.RP.getType())
                .distinct("data.metaDataFields.scopes", String.class);
            scopes.forEach(scope -> mongoTemplate.insert(new Scope(scope, new HashMap<>(), new HashMap<>())));
        }
    }

    @ChangeSet(order = "004", id = "removeSessions", author = "okke.harsta@surf.nl", runAlways = true)
    public void removeSessions(MongockTemplate mongoTemplate) {
        mongoTemplate.remove(new Query(), "sessions");
    }

    @ChangeSet(order = "005", id = "revisionCreatedIndex", author = "okke.harsta@surf.nl")
    public void revisionCreatedIndex(MongockTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            mongoTemplate.indexOps(entityType.getType())
                .ensureIndex(new Index("revision.created", Sort.Direction.DESC));
        });
    }

    @ChangeSet(order = "005", id = "revisionTerminatedIndex", author = "okke.harsta@surf.nl")
    public void revisionTerminatedIndex(MongockTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            mongoTemplate.indexOps(entityType.getType().concat(REVISION_POSTFIX))
                .ensureIndex(new Index("revision.terminated", Sort.Direction.DESC));
        });
    }

    @ChangeSet(order = "006", id = "caseInsensitiveIndexEntityID", author = "okke.harsta@surf.nl")
    public void caseInsensitiveIndexEntityID(MongockTemplate mongoTemplate) {
        Stream.of(EntityType.values())
            .filter(entityType -> !entityType.equals(EntityType.STT))
            .map(EntityType::getType).forEach(val -> {
                IndexOperations indexOperations = mongoTemplate.indexOps(val);
                List<IndexInfo> indexInfo = indexOperations.getIndexInfo();
                if (indexInfo.stream().anyMatch(info -> info.getName().equals("data.entityid_1"))) {
                    indexOperations.dropIndex("data.entityid_1");
                }
                indexOperations.ensureIndex(new Index("data.entityid", Sort.Direction.ASC).unique()
                    .collation(Collation.of("en").strength(2)));
            });
    }

    @SneakyThrows
    @ChangeSet(order = "007", id = "moveResourceServers", author = "okke.harsta@surf.nl")
    public void moveResourceServers(MongockTemplate mongoTemplate, MetaDataAutoConfiguration metaDataAutoConfiguration) {
        this.doCreateSchemas(mongoTemplate, Collections.singletonList(EntityType.RS.getType()));

        Criteria criteria = Criteria.where("data.metaDataFields.isResourceServer").is(true);
        List<MetaData> resourceServers = mongoTemplate.findAllAndRemove(Query.query(criteria), MetaData.class, EntityType.RP.getType());
        Map<String, Object> schemaRepresentation = metaDataAutoConfiguration.schemaRepresentation(EntityType.RS);
        Map<String, Map<String, Object>> properties = (Map<String, Map<String, Object>>) schemaRepresentation.get("properties");
        Map<String, Object> metaDataFields = properties.get("metaDataFields");
        Map<String, Object> patternProperties = (Map<String, Object>) metaDataFields.get("patternProperties");

        List<Pattern> patterns = patternProperties.keySet().stream().map(key -> Pattern.compile(key)).collect(Collectors.toList());
        Map<String, Object> simpleProperties = (Map<String, Object>) metaDataFields.get("properties");

        resourceServers.forEach(rs -> migrateRelayingPartyToResourceServer(properties, patterns, simpleProperties, rs));
        if (!CollectionUtils.isEmpty(resourceServers)) {
            BulkWriteResult bulkWriteResult = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, MetaData.class, EntityType.RS.getType()).insert(resourceServers).execute();
            LOG.info(String.format("Migrated %s relying parties to resource server collection", bulkWriteResult.getInsertedCount()));
        }

        List<String> identifiers = resourceServers.stream().map(metaData -> metaData.getId()).collect(Collectors.toList());
        Criteria revisionCriteria = Criteria.where("revision.parentId").in(identifiers);
        List<MetaData> revisions = mongoTemplate.findAllAndRemove(Query.query(revisionCriteria), MetaData.class, EntityType.RP.getType().concat(REVISION_POSTFIX));

        revisions.forEach(rev -> migrateRelayingPartyToResourceServer(properties, patterns, simpleProperties, rev));
        if (!CollectionUtils.isEmpty(revisions)) {

            BulkWriteResult bulkWriteResultRevisions = mongoTemplate.bulkOps(BulkOperations.BulkMode.ORDERED, MetaData.class, EntityType.RS.getType().concat(REVISION_POSTFIX)).insert(revisions).execute();
            LOG.info(String.format("Migrated %s relying party revisions to resource server revisions collection", bulkWriteResultRevisions.getInsertedCount()));
        }
    }

    @ChangeSet(order = "008", id = "removeExtraneousKeys", author = "okke.harsta@surf.nl")
    public void removeExtraneousKeys(MongockTemplate mongoTemplate) {
        List<MetaData> relyingParties = mongoTemplate.findAll(MetaData.class, EntityType.RP.getType());
        List<String> extraneousKeysRelyingParties = Arrays.asList("scopes", "isResourceServer");
        relyingParties.forEach(rp -> {
            Map<String, Object> metaDataFields = rp.metaDataFields();
            Set<String> keySet = metaDataFields.keySet();
            if (keySet.stream().anyMatch(key -> extraneousKeysRelyingParties.contains(key))) {
                keySet.removeIf(key -> extraneousKeysRelyingParties.contains(key));
                LOG.info(String.format("Saving %s relying party where extraneousKeys are removed", rp.getData().get("entityid")));
                mongoTemplate.save(rp, EntityType.RP.getType());
            }
        });
        List<MetaData> resourceServers = mongoTemplate.findAll(MetaData.class, EntityType.RS.getType());
        List<String> extraneousKeysResourceServers = Arrays.asList("NameIDFormats:0", "NameIDFormats:1", "NameIDFormats:2");
        resourceServers.forEach(rs -> {
            Map<String, Object> metaDataFields = rs.metaDataFields();
            Set<String> keySet = metaDataFields.keySet();
            if (keySet.stream().anyMatch(key -> extraneousKeysResourceServers.contains(key))) {
                keySet.removeIf(key -> extraneousKeysResourceServers.contains(key));
                LOG.info(String.format("Saving %s relying party where extraneousKeys are removed", rs.getData().get("entityid")));
                mongoTemplate.save(rs, EntityType.RS.getType());
            }
        });
    }

    @ChangeSet(order = "009", id = "addScopeTitles", author = "okke.harsta@surf.nl")
    public void addScopeTitles(MongockTemplate mongoTemplate) {
        List<Scope> scopes = mongoTemplate.findAll(Scope.class);
        scopes.forEach(scope -> {
            scope.update(scope);
            mongoTemplate.save(scope);
        });
    }

    @ChangeSet(order = "010", id = "createChangeRequestsCollections", author = "okke.harsta@surf.nl")
    public void createChangeRequestsCollections(MongockTemplate mongoTemplate) {
        Stream.of(EntityType.values()).forEach(entityType -> {
            String revisionCollection = entityType.getType().concat(CHANGE_REQUEST_POSTFIX);
            if (!mongoTemplate.collectionExists(revisionCollection)) {
                mongoTemplate.createCollection(revisionCollection);
            }
        });
    }

    @ChangeSet(order = "011", id = "addProvisioning", author = "okke.harsta@surf.nl")
    public void addProvisioning(MongockTemplate mongoTemplate) {
        String schema = EntityType.PROV.getType();
        if (!mongoTemplate.collectionExists(schema)) {
            mongoTemplate.createCollection(schema);
        }
        String revision = schema.concat(REVISION_POSTFIX);
        if (!mongoTemplate.collectionExists(revision)) {
            mongoTemplate.createCollection(revision);
        }
        mongoTemplate.indexOps(revision).ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
        TextIndexDefinition textIndexDefinition = new TextIndexDefinition.TextIndexDefinitionBuilder()
            .onField("$**")
            .build();
        mongoTemplate.indexOps(schema).ensureIndex(textIndexDefinition);
    }

    @ChangeSet(order = "012", id = "addTextIndexRS", author = "okke.harsta@surf.nl")
    public void addTextIndexRS(MongockTemplate mongoTemplate) {
        String schema = EntityType.RS.getType();
        TextIndexDefinition textIndexDefinition = new TextIndexDefinition.TextIndexDefinitionBuilder()
            .onField("$**")
            .build();
        mongoTemplate.indexOps(schema).ensureIndex(textIndexDefinition);
    }

    @ChangeSet(order = "013", id = "addPolicy", author = "okke.harsta@surf.nl")
    public void addPolicy(MongockTemplate mongoTemplate) {
        String schema = EntityType.PDP.getType();
        if (!mongoTemplate.collectionExists(schema)) {
            mongoTemplate.createCollection(schema);
        }
        String revision = schema.concat(REVISION_POSTFIX);
        if (!mongoTemplate.collectionExists(revision)) {
            mongoTemplate.createCollection(revision);
        }
        mongoTemplate.indexOps(revision).ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
        TextIndexDefinition textIndexDefinition = new TextIndexDefinition.TextIndexDefinitionBuilder()
            .onField("$**")
            .build();
        mongoTemplate.indexOps(schema).ensureIndex(textIndexDefinition);
    }

    @ChangeSet(order = "014", id = "clearInvalidARPKeys", author = "okke.harsta@surf.nl")
    public void clearInvalidARPKeys(MongockTemplate mongoTemplate) {
        final List<String> allowedKeys = List.of("source", "value", "motivation", "release_as", "use_as_nameid");
        List.of(EntityType.RP, EntityType.SP, EntityType.STT).forEach(entityType -> {
            List<MetaData> metaDataList = mongoTemplate.findAll(MetaData.class, entityType.getType());
            metaDataList.forEach(metaData -> {
                if (metaData.getData().containsKey("arp")) {
                    Map<String, Object> arp = (Map<String, Object>) metaData.getData().get("arp");
                    if (!CollectionUtils.isEmpty(arp)) {
                        Object attributesObject = arp.get("attributes");
                        if (attributesObject instanceof List) {
                            arp.put("attributes", new HashMap<>());
                            LOG.info(String.format("Saving %s metadata type %s where ARP attributes is a List",
                                metaData.getData().get("entityid"), entityType));
                            mongoTemplate.save(metaData, entityType.getType());
                        } else if (attributesObject instanceof Map) {
                            Map<String, List<Map<String, Object>>> attributes = (Map<String, List<Map<String, Object>>>) arp.get("attributes");
                            if (!CollectionUtils.isEmpty(attributes)) {
                                AtomicBoolean needsSaving = new AtomicBoolean(false);
                                attributes.forEach((arpName, value) -> value.forEach(attribute -> {
                                    boolean useAsNameId = (boolean) attribute.getOrDefault("use_as_nameid",
                                        attribute.getOrDefault("use_as_name_id",
                                            attribute.getOrDefault("useAsNameId", false)));
                                    attribute.put("use_as_nameid", useAsNameId);
                                    String releaseAt = (String) attribute.getOrDefault("releaseAs", attribute.get("release_as"));
                                    if (StringUtils.hasText(releaseAt)) {
                                        attribute.put("release_as", releaseAt);
                                    }
                                    if (attribute.keySet().removeIf(key -> !allowedKeys.contains(key))) {
                                        needsSaving.set(true);
                                    }
                                }));
                                if (needsSaving.get()) {
                                    //We don't make a new revision as this is a cleanup action
                                    LOG.info(String.format("Saving %s metadata type %s where ARP values are fixed",
                                        metaData.getData().get("entityid"), entityType.getType()));
                                    mongoTemplate.save(metaData, entityType.getType());

                                }
                            }
                        } else {
                            LOG.info(String.format("Unknown attributes type in metadata %s type %s",
                                metaData.getData().get("entityid"), entityType));
                        }
                    }
                }
            });
        });
    }

    @ChangeSet(order = "015", id = "addSRAMServices", author = "okke.harsta@surf.nl")
    public void addSRAMServices(MongockTemplate mongoTemplate) {
        String schema = EntityType.SRAM.getType();
        if (!mongoTemplate.collectionExists(schema)) {
            mongoTemplate.createCollection(schema);
        }
        String revision = schema.concat(REVISION_POSTFIX);
        if (!mongoTemplate.collectionExists(revision)) {
            mongoTemplate.createCollection(revision);
        }
        mongoTemplate.indexOps(revision).ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
        TextIndexDefinition textIndexDefinition = new TextIndexDefinition.TextIndexDefinitionBuilder()
            .onField("$**")
            .build();
        mongoTemplate.indexOps(schema).ensureIndex(textIndexDefinition);
    }

    @ChangeSet(order = "016", id = "migrateMultiplicityKeys", author = "okke.harsta@surf.nl")
    public void migrateMultiplicityKeys(MongockTemplate mongoTemplate) {
        List<MetaData> identityProviders = mongoTemplate.findAll(MetaData.class, EntityType.IDP.getType());
        identityProviders.forEach(metaData -> {
            Map<String, Object> metaDataFields = metaData.metaDataFields();
            List.of("en", "nl", "pt").forEach(language -> {
                String name = (String) metaDataFields.get("name:" + language);
                if (StringUtils.hasText(name)) {
                    metaDataFields.put("DiscoveryName:0:" + language, name);
                }
                String keywords = (String) metaDataFields.get("keywords:" + language);
                if (StringUtils.hasText(keywords)) {
                    metaDataFields.put("keywords:0:" + language, keywords);
                }
                metaDataFields.remove("keywords:" + language);
            });
            //We don't make a new revision as this is a cleanup action
            LOG.info(String.format("Saving %s metadata type %s where DiscoveryName and keywords are migrated",
                metaData.getData().get("entityid"), EntityType.IDP.getType()));
            mongoTemplate.save(metaData, EntityType.IDP.getType());
        });
    }

    @SneakyThrows
    @ChangeSet(order = "017", id = "migrateApplicationTags", author = "okke.harsta@surf.nl")
    public void migrateApplicationTags(MongockTemplate mongoTemplate) {
        //All of the occurrences of coin:ss:type_of_service:en must be duplicated to application_tags
        String tags = IOUtils.toString(new ClassPathResource("migration.tags.csv").getInputStream(), Charset.defaultCharset());
        String[] lines = tags.split("\\R");
        Map<String, String> mappings = new HashMap<>();
        Stream.of(lines).forEach(s -> {
            String[] splitted = s.split(",");
            mappings.put(splitted[0].trim().toLowerCase(), splitted[1].trim().toLowerCase());
        });
        List<MetaData> serviceProviders = mongoTemplate.findAll(MetaData.class, EntityType.SP.getType());
        List<MetaData> relyingParties = mongoTemplate.findAll(MetaData.class, EntityType.RP.getType());
        serviceProviders.addAll(relyingParties);
        serviceProviders.forEach(metaData -> {
            Map<String, Object> metaDataFields = metaData.metaDataFields();
            //We only need to check the English ones, as we store application_tags language agnostic
            String value = (String) metaDataFields.get("coin:ss:type_of_service:en");
            if (StringUtils.hasText(value)) {
                //can be comma separated
                List<String> applicationTags = new ArrayList<>();
                Stream.of(value.split(",")).forEach(s -> {
                    String key = s.trim().toLowerCase();
                    if (mappings.containsKey(key)) {
                        applicationTags.add(mappings.get(key));
                    }
                });
                if (!applicationTags.isEmpty()) {
                    metaDataFields.put("application_tags", applicationTags);
                    //We don't make a new revision as this is a cleanup action
                    LOG.info(String.format("Saving %s metadata type %s where coin:ss:type_of_service is duplicated in application tags",
                        metaData.getData().get("entityid"), metaData.getType()));
                    mongoTemplate.save(metaData, metaData.getType());
                }
            }
        });
    }

    @SneakyThrows
    @ChangeSet(order = "018", id = "removePdpPolicyRequired", author = "okke.harsta@surf.nl")
    public void removePdpPolicyRequired(MongockTemplate mongoTemplate) {
        String pdpPolicyRequired = "coin:policy_enforcement_decision_required";
        Arrays.asList(EntityType.SP, EntityType.RP, EntityType.IDP, EntityType.SRAM, EntityType.STT)
            .forEach(entityType -> {
                List<MetaData> metaDataList = mongoTemplate.findAll(MetaData.class, entityType.getType());
                metaDataList.forEach(metaData -> {
                    Map<String, Object> metaDataFields = metaData.metaDataFields();
                    if (metaDataFields.containsKey(pdpPolicyRequired)) {
                        metaDataFields.remove(pdpPolicyRequired);
                        LOG.info("Removed {} from entity {} in collection {}",
                            pdpPolicyRequired, metaData.getData().get("entityid"), entityType.getType());
                        //We don't make a new revision as this is a cleanup action
                        mongoTemplate.save(metaData, entityType.getType());
                    }
                });
            });
    }

    private void migrateRelayingPartyToResourceServer(Map<String, Map<String, Object>> properties, List<Pattern> patterns, Map<String, Object> simpleProperties, MetaData rs) {
        rs.setType(EntityType.RS.getType());
        rs.getData().entrySet().removeIf(entry -> !properties.containsKey(entry.getKey()));
        rs.metaDataFields().entrySet().removeIf(entry -> !simpleProperties.containsKey(entry.getKey()) && patterns.stream().noneMatch(pattern -> pattern.matcher(entry.getKey()).matches()));
    }

    private void doCreateSchemas(MongockTemplate mongoTemplate, List<String> connectionTypes) {
        connectionTypes.forEach(schema -> {
            if (!mongoTemplate.collectionExists(schema)) {
                mongoTemplate.createCollection(schema);
                String revision = schema.concat(REVISION_POSTFIX);
                mongoTemplate.createCollection(revision);
                mongoTemplate.indexOps(revision).ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
            }
        });
        connectionTypes.forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.getIndexInfo().stream()
                .filter(indexInfo -> indexInfo.getName().contains("data.eid"))
                .forEach(indexInfo -> indexOps.dropIndex(indexInfo.getName()));
            indexOps.ensureIndex(new Index("data.eid", Sort.Direction.ASC).unique());

            indexOps.getIndexInfo().stream().filter(indexInfo -> indexInfo.getName().contains("entityid")).forEach(indexInfo -> indexOps.dropIndex(indexInfo.getName()));
            indexOps.ensureIndex(new Index("data.entityid", Sort.Direction.ASC).unique()
                .collation(Collation.of("en").strength(2)));
            indexOps.ensureIndex(new Index("data.state", Sort.Direction.ASC));
            if (!collection.equals(EntityType.RS.getType())) {
                indexOps.ensureIndex(new Index("data.allowedall", Sort.Direction.ASC));
                indexOps.ensureIndex(new Index("data.allowedEntities.name", Sort.Direction.ASC));
                indexOps.ensureIndex(new Index("data.metaDataFields.coin:institution_id", Sort.Direction.ASC));
            }
        });
        connectionTypes.stream().map(s -> s + "_revision").forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
        });
    }

}
