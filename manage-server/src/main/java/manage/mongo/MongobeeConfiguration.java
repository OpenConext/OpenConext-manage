package manage.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mongobee.Mongobee;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import manage.conf.IndexConfiguration;
import manage.conf.MetaDataAutoConfiguration;
import manage.migration.EntityType;
import manage.model.MetaData;
import manage.repository.MetaDataRepository;
import org.apache.commons.io.IOUtils;
import org.bson.types.Code;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.IndexOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

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
    }

    @ChangeSet(order = "002", id = "createSingleTenantTemplates", author = "Okke Harsta")
    public void createSingleTenantTemplates(MongoTemplate mongoTemplate) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:single_tenant_templates/*.json");
        final AtomicLong id = new AtomicLong();
        Arrays.asList(resources).forEach(res -> this.createSingleTenantTemplate(mongoTemplate, res, objectMapper,
            id.incrementAndGet()));

    }

    @ChangeSet(order = "003", id = "reCreateSingleTenantTemplates", author = "Okke Harsta")
    public void reCreateSingleTenantTemplates(MongoTemplate mongoTemplate) throws IOException {
        mongoTemplate.findAllAndRemove(new Query(), "single_tenant_template");
        mongoTemplate.findAllAndRemove(new Query(), "single_tenant_template_revision");
        createSingleTenantTemplates(mongoTemplate);
    }

    @ChangeSet(order = "004", id = "importCSA", author = "Okke Harsta")
    public void importCsaSettings(MongoTemplate mongoTemplate) throws Exception {
        doImportCsaSettings(mongoTemplate);
    }

    @ChangeSet(order = "005", id = "reImportCSA", author = "Okke Harsta")
    public void reImportCsaSettings(MongoTemplate mongoTemplate) throws Exception {
        doImportCsaSettings(mongoTemplate);
    }

    private void doImportCsaSettings(MongoTemplate mongoTemplate) throws IOException {
        String type = EntityType.SP.getType();
        String content = IOUtils.toString(new ClassPathResource("csa_export/compound_service_provider.csv")
            .getInputStream(), Charset
            .defaultCharset());
        List<String> lines = Arrays.asList(content.split("\n"));

        Map<String, String> mappedLicenseStatus = new HashMap<>();
        mappedLicenseStatus.put("HAS_LICENSE_SP", "license_required_by_service_provider");
        mappedLicenseStatus.put("NOT_NEEDED", "license_not_required");
        mappedLicenseStatus.put("HAS_LICENSE_SURFMARKET", "license_available_through_surfmarket");
        mappedLicenseStatus.put("UNKNOWN", "license_required_by_service_provider");

        lines.forEach(l -> {
            List<String> columns = Arrays.asList(l.split(","));
            //service_provider_entity_id, normenkader_present, license_status, strong_authentication
            String entityId = columns.get(0);
            Query query = new Query();
            query.addCriteria(Criteria.where("data.entityid").is(entityId));
            List<MetaData> metaDatas = mongoTemplate.find(query, MetaData.class, type);
            if (metaDatas != null && metaDatas.size() > 0) {
                MetaData metaData = metaDatas.get(0);
                Map<String, Object> metaDataFields = (Map<String, Object>) metaData.getData().get("metaDataFields");
                metaDataFields.put("coin:privacy:gdpr_is_in_wiki", columns.get(1));

                String licenseStatus = mappedLicenseStatus.getOrDefault(columns.get(2),
                    "license_required_by_service_provider");
                metaDataFields.put("coin:ss:license_status", licenseStatus);
                metaDataFields.remove("coin:license_status");

                metaDataFields.put("coin:ss:supports_strong_authentication", columns.get(3));
                metaDataFields.remove("coin:requires_strong_authentication");
                metaDataFields.remove("coin:supports_strong_authentication");

                MetaData previous = mongoTemplate.findById(metaData.getId(), MetaData.class, type);
                previous.revision(UUID.randomUUID().toString());
                mongoTemplate.insert(previous, previous.getType());
                metaData.promoteToLatest("CSA import migration");
                mongoTemplate.save(metaData, metaData.getType());
                LOG.info("Migrated {} to new revision in CSA import", entityId);
            }
        });
    }

    @ChangeSet(order = "006", id = "addValueToDisableConsent", author = "Okke Harsta")
    public void addValueToDisableConsent(MongoTemplate mongoTemplate) throws Exception {
        List<MetaData> allIdPs = mongoTemplate.findAll(MetaData.class, EntityType.IDP.getType());
        allIdPs.forEach(idp -> ((ArrayList<Map<String, String>>) idp.getData()
            .getOrDefault("disableConsent", new ArrayList<Map<String, String>>()))
            .forEach(dc -> {
                dc.put("type", "no_consent");
                dc.put("explanation", "");
            }));
        allIdPs.stream()
            .filter(idp -> !List.class.cast(idp.getData().getOrDefault("disableConsent", new ArrayList<Map<String,
                String>>())).isEmpty())
            .forEach(idp -> {
                MetaData previous = mongoTemplate.findById(idp.getId(), MetaData.class, EntityType.IDP.getType());
                previous.revision(UUID.randomUUID().toString());
                mongoTemplate.insert(previous, previous.getType());
                idp.promoteToLatest("Add type / explanation to disableConsent entries");
                mongoTemplate.save(idp, idp.getType());
                LOG.info("Migrated {} to new revision in CSA import", idp.getData().get("entityid"));
            });
    }

    @ChangeSet(order = "007", id = "importFacetsInformation", author = "Okke Harsta")
    public void importFacetsInformation(MongoTemplate mongoTemplate) throws IOException {
        String type = EntityType.SP.getType();
        String content = IOUtils.toString(new ClassPathResource("csa_export/facets.csv").getInputStream(), Charset
            .defaultCharset());
        List<String> lines = Arrays.asList(content.split("\n"));

        Map<String, List<TypeOfService>> typeOfServices = lines.stream().map(s -> {
            //entity_id, type_of_service, language
            List<String> columns = Arrays.asList(s.split(","));
            return new TypeOfService(columns.get(0), columns.get(1), columns.get(2));
        }).collect(Collectors.groupingBy(TypeOfService::getLang));
        Map<String, List<TypeOfService>> nl = typeOfServices.get("nl").stream().collect(Collectors.groupingBy
            (TypeOfService::getEntityId));
        Map<String, List<TypeOfService>> en = typeOfServices.get("en").stream().collect(Collectors.groupingBy
            (TypeOfService::getEntityId));
        addTypeOfService(mongoTemplate, type, nl, "nl");
        addTypeOfService(mongoTemplate, type, en, "en");
    }

    @ChangeSet(order = "008", id = "addEIDToNewEntities", author = "Okke Harsta")
    public void addEIDToNewEntitiesToParents(MongoTemplate mongoTemplate) throws IOException {
        Query query = new Query();
        query.addCriteria(Criteria.where("data.eid").exists(false));

        Arrays.asList(EntityType.values()).forEach(type -> {
            String collectionName = type.getType();
            List<Map> parentProviders = mongoTemplate.find(query, Map.class, collectionName);
            parentProviders.forEach(provider -> {
                long eid = this.highestEid(mongoTemplate, collectionName) + 1;
                Map.class.cast(provider.get("data")).put("eid", eid);
                mongoTemplate.save(provider, collectionName);
                LOG.info("Add eid {} to parent provider {}", eid, provider.get("_id"));
            });
        });
    }

    @ChangeSet(order = "009", id = "addEIDToNewEntitiesToRevisions", author = "Okke Harsta")
    public void addEIDToNewEntitiesToChildren(MongoTemplate mongoTemplate) throws IOException {
        Query query = new Query();
        query.addCriteria(Criteria.where("data.eid").exists(false));

        //All parent providers have a valid eid due to change set 008 and we can process the revisions
        Map<Object, Long> parentIdToEidMap = new HashMap<>();
        Arrays.asList(EntityType.values()).forEach(type -> {
            String collectionName = type.getType().concat(REVISION_POSTFIX);
            List<Map> childProviders = mongoTemplate.find(query, Map.class, collectionName);
            childProviders.forEach(childProvider -> {
                Map revision = Map.class.cast(childProvider.get("revision"));
                Object parentId = revision.get("parentId");
                Map parent = mongoTemplate.findById(parentId, Map.class, type.getType());
                if (parent != null) {
                    Object eid = Map.class.cast(parent.get("data")).get("eid");

                    Map childData = Map.class.cast(childProvider.get("data"));
                    childData.put("eid", eid);
                    mongoTemplate.save(childProvider, collectionName);
                    LOG.info("Add eid {} to child provider {}", eid, childProvider.get("_id"));
                } else {
                    long eid;
                    if (parentIdToEidMap.containsKey(parentId)) {
                        LOG.info("Re-using new eid for child provider {} because parent {} is deleted", childProvider
                            .get("_id"), parentId);
                        eid = parentIdToEidMap.get(parentId);
                    } else {
                        eid = this.highestEid(mongoTemplate, collectionName) + 1;
                        LOG.info("Calculating new eid for child provider {} because parent {} is deleted", childProvider
                            .get("_id"), parentId);
                        parentIdToEidMap.put(parentId, eid);
                    }
                    Map childData = Map.class.cast(childProvider.get("data"));
                    childData.put("eid", eid);
                    mongoTemplate.save(childProvider, collectionName);
                    LOG.info("Add new eid {} to child provider {}", eid, childProvider.get("_id"));
                }
            });
        });
    }


    @ChangeSet(order = "010", id = "renameRequiresToSupportsStepUp", author = "Okke Harsta")
    public void renameRequiresToSupportsStepUp(MongoTemplate mongoTemplate) throws IOException {
        Query query = new Query();
        query.addCriteria(Criteria.where("data.metaDataFields.coin:requires_strong_authentication").exists(true));
        List<MetaData> entities = mongoTemplate.find(query, MetaData.class, EntityType.SP.getType());
        entities.forEach(entity -> {
            Map<String, Object> metaDataFields = (Map<String, Object>) entity.getData().get("metaDataFields");
            Object supportsStrongAuthentication = metaDataFields.get("coin:requires_strong_authentication");
            metaDataFields.put("coin:supports_strong_authentication", supportsStrongAuthentication);
            metaDataFields.remove("coin:requires_strong_authentication");
            LOG.info("Saving metadata {} with supports_strong_authentication", entity.getId());
            mongoTemplate.save(entity, EntityType.SP.getType());
        });
    }

    @ChangeSet(order = "011", id = "splitConsentDisablingIntoMultiLanguage", author = "Okke Harsta")
    public void splitConsentDisablingIntoMultiLanguage(MongoTemplate mongoTemplate) throws IOException {
        Query query = new Query();
        query.addCriteria(Criteria.where("data.disableConsent").exists(true).not().size(0));
        List<MetaData> entities = mongoTemplate.find(query, MetaData.class, EntityType.IDP.getType());
        entities.forEach(entity -> {
            List<Map<String, Object>> disableConsents = (List<Map<String, Object>>) entity.getData().get
                ("disableConsent");
            disableConsents.forEach(disableConsent -> {
                Object explanation = disableConsent.get("explanation");
                disableConsent.put("explanation:en", explanation);
                disableConsent.put("explanation:nl", explanation);
                disableConsent.remove("explanation");
            });
            LOG.info("Saving metadata {} with multi language", entity.getId());
            mongoTemplate.save(entity, EntityType.IDP.getType());
        });
    }

    @ChangeSet(order = "012", id = "finalImportSingleTenantTemplates", author = "Okke Harsta")
    public void importSingleTenantTemplates(MongoTemplate mongoTemplate) throws IOException {
        mongoTemplate.findAllAndRemove(new Query(), "single_tenant_template");
        mongoTemplate.findAllAndRemove(new Query(), "single_tenant_template_revision");
        createSingleTenantTemplates(mongoTemplate);
    }

    @ChangeSet(order = "013", id = "finalImportCsaSettings", author = "Okke Harsta")
    public void finalImportCsaSettings(MongoTemplate mongoTemplate) throws Exception {
        doImportCsaSettings(mongoTemplate);
    }

    @ChangeSet(order = "014", id = "finalImportFacetsInformation", author = "Okke Harsta")
    public void finalImportFacetsInformation(MongoTemplate mongoTemplate) throws IOException {
        this.importFacetsInformation(mongoTemplate);
    }

    @ChangeSet(order = "015", id = "importWikiURLs", author = "Okke Harsta")
    public void importWikiUrls(MongoTemplate mongoTemplate) throws IOException {
        String type = EntityType.SP.getType();
        String content = IOUtils.toString(new ClassPathResource("csa_export/wiki_url.csv").getInputStream(), Charset
            .defaultCharset());
        List<String> lines = Arrays.asList(content.split("\n"));
        Map<String, List<WikiUrlService>> wikiUrlServices = lines.stream().map(s -> {
            //entity_id, ordinal_lang, key, wiki_url
            List<String> columns = Arrays.asList(s.split(","));
            return new WikiUrlService(columns.get(0), columns.get(3), columns.get(1).equals("18") ? "en" : "nl");
        }).collect(Collectors.groupingBy(WikiUrlService::getEntityId));

        wikiUrlServices.forEach((entityId, urls) -> {
            Query query = new Query();
            query.addCriteria(Criteria.where("data.entityid").is(entityId));
            List<MetaData> metaDatas = mongoTemplate.find(query, MetaData.class, type);
            if (metaDatas != null && metaDatas.size() > 0) {
                MetaData metaData = metaDatas.get(0);
                Map<String, Object> metaDataFields = (Map<String, Object>) metaData.getData().get("metaDataFields");
                urls.forEach(wikiUrlService -> {
                    metaDataFields.put("coin:ss:wiki_url:" + wikiUrlService.getLang(), wikiUrlService.getValue());
                });
                metaDataFields.remove("coin:wiki_url:en");
                metaDataFields.remove("coin:wiki_url:nl");
                MetaData previous = mongoTemplate.findById(metaData.getId(), MetaData.class, type);
                previous.revision(UUID.randomUUID().toString());
                mongoTemplate.insert(previous, previous.getType());
                metaData.promoteToLatest("CSA wiki URL import migration");
                mongoTemplate.save(metaData, metaData.getType());
                LOG.info("Migrated {} to new revision in CSA import", entityId);
            }

        });
    }

    @ChangeSet(order = "016", id = "createIndexes", author = "Okke Harsta")
    public void createIndexes(MongoTemplate mongoTemplate) {
        Arrays.asList("saml20_sp","saml20_idp").forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.dropIndex("field_entityid");
            indexOps.ensureIndex(new Index("data.entityid", Sort.Direction.ASC).unique());
            indexOps.ensureIndex(new Index("data.state", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("data.allowedall", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("data.allowedEntities.name", Sort.Direction.ASC));
            indexOps.ensureIndex(new Index("metaDataFields.coin:institution_id", Sort.Direction.ASC));
        });
        Arrays.asList("saml20_sp_revision","saml20_idp_revision").forEach(collection -> {
            IndexOperations indexOps = mongoTemplate.indexOps(collection);
            indexOps.ensureIndex(new Index("revision.parentId", Sort.Direction.ASC));
        });
    }

    @ChangeSet(order = "017", id = "updateEidForDuplicates", author = "Okke Harsta")
    public void updateEidForDuplicates(MongoTemplate mongoTemplate) {
        List<MetaData> identityProviders = mongoTemplate.findAll(MetaData.class, "saml20_idp");
        List<MetaData> serviceProviders = mongoTemplate.findAll(MetaData.class, "saml20_sp");

        long max = Math.max(highestEid(mongoTemplate, "saml20_idp"), highestEid(mongoTemplate, "saml20_sp"));

        if (mongoTemplate.collectionExists("sequences")) {
            mongoTemplate.dropCollection("sequences");
        }
        mongoTemplate.createCollection("sequences");
        LOG.info("Creating sequence collection with new start seq {}", max + 1L);
        mongoTemplate.save(new Sequence("sequence", max + 1L));

        Set<Long> eids = serviceProviders.stream().map(this::eid).collect(Collectors.toSet());
        List<MetaData> duplicates = identityProviders.stream().filter(m -> !eids.add(eid(m))).collect(toList());

        MetaDataRepository repository = new MetaDataRepository(mongoTemplate);

        duplicates.forEach(metaData -> {
            Object currentEid = metaData.getData().get("eid");

            Long newEid = repository.incrementEid();
            metaData.getData().put("eid", newEid);

            repository.update(metaData);

            LOG.info("Updated identityProvider {} with current eid {} to new eid {}",
                metaData.getData().get("entityid"),
                currentEid,
                newEid);

            List<MetaData> revisions = repository.revisions(metaData.getType().concat(REVISION_POSTFIX), metaData.getId());

            revisions.forEach(revision -> {
                Object currentRevEid = metaData.getData().get("eid");

                revision.getData().put("eid", newEid);
                repository.update(revision);

                LOG.info("Updated identityProvider revision {} with current eid {} to new eid {}",
                    revision.getData().get("entityid"),
                    currentRevEid,
                    newEid);
            });
        });
    }

    private Long highestEid(MongoTemplate mongoTemplate, String type) {
        Query query = new Query().limit(1).with(new Sort(Sort.Direction.DESC, "data.eid"));
        query.fields().include("data.eid");
        Map res = mongoTemplate.findOne(query, Map.class, type);
        return Long.valueOf(Map.class.cast(res.get("data")).get("eid").toString());
    }

    private Long eid(MetaData metaData) {
        return Long.valueOf(metaData.getData().get("eid").toString());
    }
    private void addTypeOfService(MongoTemplate mongoTemplate, String type, Map<String, List<TypeOfService>> nl,
                                  String lang) {
        nl.keySet().forEach(entityId -> {
            Query query = new Query();
            query.addCriteria(Criteria.where("data.entityid").is(entityId));
            List<MetaData> metaDatas = mongoTemplate.find(query, MetaData.class, type);
            if (metaDatas != null && metaDatas.size() > 0) {
                MetaData metaData = metaDatas.get(0);
                Map<String, Object> metaDataFields = (Map<String, Object>) metaData.getData().get("metaDataFields");
                metaDataFields.put("coin:ss:type_of_service:" + lang, nl.get(entityId).stream().map
                    (TypeOfService::getValue).collect(Collectors.toSet()).stream().collect(Collectors.joining(",")));
                metaDataFields.remove("coin:type_of_service:en");
                metaDataFields.remove("coin:type_of_service:nl");
                MetaData previous = mongoTemplate.findById(metaData.getId(), MetaData.class, type);
                previous.revision(UUID.randomUUID().toString());
                mongoTemplate.insert(previous, previous.getType());
                metaData.promoteToLatest("CSA facet import migration");
                mongoTemplate.save(metaData, metaData.getType());
                LOG.info("Migrated {} to new revision in CSA import", entityId);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void createSingleTenantTemplate(MongoTemplate mongoTemplate, Resource resource, ObjectMapper
        objectMapper, Long currentEid) {
        final Map<String, Object> template = readTemplate(resource, objectMapper);
        Map<String, Object> data = new HashMap<>();
        data.put("entityid", template.get("entityid"));
        data.put("state", template.get("workflowState"));
        HashMap<String, Object> arp = new HashMap<>();
        data.put("arp", arp);
        boolean enabled = template.containsKey("attributes");
        arp.put("enabled", enabled);
        Map<String, List<Map<String, Object>>> attributes = new HashMap<>();
        if (enabled) {
            Map<String, Object> source = new HashMap<>();
            source.put("source", "idp");
            source.put("value", "*");
            List<Map<String, Object>> values = Collections.singletonList(source);
            List.class.cast(template.get("attributes")).forEach(attr -> attributes.put(((String) attr)
                .replaceAll("\\.", "@"), values));

        }
        arp.put("attributes", attributes);

        Arrays.asList(new String[]{"entityid", "workflowState", "attributes"}).forEach(none -> template.remove(none));
        template.keySet().forEach(key -> {
            if (key.contains(".")) {
                template.put(key.replaceAll("\\.", "@"), template.get(key));
                template.remove(key);
            }
        });
        data.put("metaDataFields", template);

        MetaData metaData = new MetaData("single_tenant_template", data);

        metaData.initial(UUID.randomUUID().toString(), "auto-generated", currentEid);
        mongoTemplate.insert(metaData, metaData.getType());
    }

    private Map<String, Object> readTemplate(Resource resource, ObjectMapper objectMapper) {
        Map<String, Object> template;
        try {
            template = objectMapper.readValue(resource.getInputStream(), Map.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return template;
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
