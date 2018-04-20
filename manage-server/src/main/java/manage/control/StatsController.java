package manage.control;

import lombok.EqualsAndHashCode;
import manage.api.APIUser;
import manage.migration.EntityType;
import manage.repository.MetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.toList;
import static manage.mongo.MongobeeConfiguration.REVISION_POSTFIX;

@RestController
public class StatsController {

    private static final Logger LOG = LoggerFactory.getLogger(StatsController.class);

    private MetaDataRepository metaDataRepository;

    @Autowired
    public StatsController(MetaDataRepository metaDataRepository) {
        this.metaDataRepository = metaDataRepository;
    }

    @GetMapping("/internal/stats/revisions")
    public List<Map> revisions(APIUser apiUser) {
        LOG.info("Revisions request by {}", apiUser.getName());
        Query query = getQueryWithDefaultFields(new Query());

        List<Map> providers = new ArrayList<>();
        Arrays.asList(EntityType.values()).forEach(type -> {
            Arrays.asList(new String[]{type.getType(), type.getType().concat(REVISION_POSTFIX)}).forEach
                (collectionName -> providers.addAll(metaDataRepository.getMongoTemplate().find(query, Map.class,
                    collectionName)));
        });

        LOG.info("Revisions request by {} returning {} providers", apiUser.getName(), providers.size());
        return providers;
    }

    @GetMapping("/internal/stats/uniques/{type}")
    public List<Map> uniques(@PathVariable("type") String type, APIUser apiUser) {
        LOG.info("Uniques request for type {} by {}", type, apiUser.getName());
        List<EntityType> entityTypes = Arrays.asList(EntityType.values());
        Assert.isTrue(entityTypes.stream().anyMatch(entityType -> entityType.getType().equals
            (type)), String.format("Unknown type metadata, allowed are %s", entityTypes.stream().map
            (EntityType::getType).collect(toList())));

        List<Map> result = new ArrayList<>();

        List<Map> providers = metaDataRepository.getMongoTemplate().find(getQueryWithDefaultFields(new Query()), Map
            .class, type);
        providers.forEach(provider -> {
            result.add(provider);

            Set<ProviderIdentifier> subResult = new HashSet<>();
            ProviderIdentifier parent = new ProviderIdentifier(provider);
            subResult.add(parent);

            String parentId = String.class.cast(provider.get("_id"));
            Query query = getQueryWithDefaultFields(new Query(Criteria.where("revision.parentId").is(parentId)).with
                (new Sort(Sort.Direction.ASC, "revision.number")));
            List<Map> revisions = metaDataRepository.getMongoTemplate().find(query, Map.class, type.concat(REVISION_POSTFIX));

            //Traditional for loop to set the end date
            for (int i = 0; i < revisions.size(); i++) {
                Map revision = revisions.get(i);
                Object ended = (revisions.size() == i + 1) ? revision(provider).get("created") :
                    revision(revisions.get(i + 1)).get("created");
                revision(revision).put("ended", ended);

                ProviderIdentifier providerIdentifier = new ProviderIdentifier(revision);
                if (subResult.add(providerIdentifier)) {
                    result.add(revision);
                }
            }
        });

        return result;
    }

    private Map revision(Map provider) {
        return Map.class.cast(provider.get("revision"));
    }

    private Query getQueryWithDefaultFields(Query query) {
        query.with(new Sort(Sort.Direction.ASC, "data.eid", "revision.number"))
            .fields()
            .include("data.eid")
            .include("revision.number")
            .include("data.state")
            .include("type")
            .include("revision.created")
            .include("revision.terminated")
            .include("data.entityid")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.name:nl")
            .include("data.metaDataFields.coin:institution_id");
        return query;
    }

    private Long metadataId(Map map, String parent, String identifier) {
        Object id = Map.class.cast(map.get(parent)).get(identifier);
        if (id instanceof Integer) {
            return Long.valueOf(Integer.class.cast(id));
        }
        if (id instanceof Long) {
            return Long.valueOf(Long.class.cast(id));
        }
        if (id == null) {
            throw new RuntimeException(String.format("Identifier %s does not exists in Map %s", identifier, map));
        }
        throw new RuntimeException(String.format("Identifier %s exists in Map %s but is of type %s", identifier, map,
            id.getClass().getName()));
    }

    @EqualsAndHashCode
    private static class ProviderIdentifier {
        //Every revision that has a different entityID, state or institution_id is considered a different entity
        public String entityId;
        public String state;
        public String institutionId;

        public ProviderIdentifier(Map provider) {
            Map data = Map.class.cast(provider.get("data"));
            this.entityId = String.class.cast(data.get("entityid"));
            this.institutionId = (String) Map.class.cast(data.getOrDefault("metaDataFields", new HashMap<>())).get
                ("coin:institution_id");
            this.state = String.class.cast(data.get("entityid"));
        }

    }
}
