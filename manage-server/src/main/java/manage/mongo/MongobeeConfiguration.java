package manage.mongo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mongobee.Mongobee;
import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import manage.conf.IndexConfiguration;
import manage.conf.MetaDataAutoConfiguration;
import manage.migration.EntityType;
import manage.migration.JanusMigration;
import manage.model.MetaData;
import org.apache.commons.io.IOUtils;
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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexDefinition;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Configuration
@ChangeLog
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

    @ChangeSet(order = "001", id = "createCollections", author = "Okke Harsta", runAlways = true)
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
        Arrays.asList(resources).forEach(res -> this.createSingleTenantTemplate(mongoTemplate, res, objectMapper));

    }

    @ChangeSet(order = "003", id = "reCreateSingleTenantTemplates", author = "Okke Harsta")
    public void reCreateSingleTenantTemplates(MongoTemplate mongoTemplate) throws IOException {
        mongoTemplate.findAllAndRemove(new Query(),"single_tenant_template");
        mongoTemplate.findAllAndRemove(new Query(),"single_tenant_template_revision");
        createSingleTenantTemplates(mongoTemplate);
    }

    @ChangeSet(order = "004", id= "importCSA", author = "Okke Harsta")
    public void inportCsaSettings(MongoTemplate mongoTemplate) throws Exception {
        String type = EntityType.SP.getType();
        String content = IOUtils.toString(new ClassPathResource("csa_export/csp.csv").getInputStream(), Charset.defaultCharset());
        List<String> lines = Arrays.asList(content.split("\n"));

        Map<String, String> mappedLicenseStatus = new HashMap<>();
        mappedLicenseStatus.put("HAS_LICENSE_SP", "license_required_by_service_provider");
        mappedLicenseStatus.put("NOT_NEEDED", "license_not_required");
        mappedLicenseStatus.put("HAS_LICENSE_SURFMARKET", "license_available_through_surfmarket");
        mappedLicenseStatus.put("UNKNOWN", "license_unknown");

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
                boolean normenKaderPresent = columns.get(1).equals("1");
                metaDataFields.put("coin:privacy:gdpr_is_in_wiki", normenKaderPresent);

                String licenseStatus = mappedLicenseStatus.getOrDefault(columns.get(2), "license_required_by_service_provider");
                metaDataFields.put("coin:license_status", licenseStatus);

                boolean strongAuthentication = columns.get(3).equals("1");
                metaDataFields.put("coin:requires_strong_authentication", strongAuthentication);

                MetaData previous = mongoTemplate.findById(metaData.getId(), MetaData.class, type);
                previous.revision(UUID.randomUUID().toString());
                mongoTemplate.insert(previous, previous.getType());
                metaData.promoteToLatest("CSA import migration");
                mongoTemplate.save(metaData, metaData.getType());
                LOG.info("Migrated {} to new revision in CSA import", entityId);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void createSingleTenantTemplate(MongoTemplate mongoTemplate, Resource resource, ObjectMapper objectMapper) {
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
        metaData.initial(UUID.randomUUID().toString(), "auto-generated");
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
