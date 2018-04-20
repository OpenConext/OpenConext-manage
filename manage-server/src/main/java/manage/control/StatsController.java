package manage.control;

import manage.api.APIUser;
import manage.migration.EntityType;
import manage.repository.MetaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public List<Map> post(APIUser apiUser) {
        LOG.info("Revisions request by {}", apiUser.getName());
        Query query = new Query();
        query
            .fields()
            .include("data.eid")
            .include("revision.number")
            .include("data.state")
            .include("data.type")
            .include("revision.created")
            .include("data.entityid")
            .include("data.metaDataFields.name:en")
            .include("data.metaDataFields.name:nl")
            .include("data.metaDataFields.coin:institution_id");

        List<Map> providers = new ArrayList<>();
        Arrays.asList(EntityType.values()).forEach(type -> {
            Arrays.asList(new String[]{type.getType(), type.getType().concat(REVISION_POSTFIX)}).forEach
                (collectionName -> providers.addAll(metaDataRepository.getMongoTemplate().find(query, Map.class,
                    collectionName)));
        });

        Collections.sort(providers, (Map m1, Map m2) -> {
            Long eid1 = metadataId(m1, "data", "eid");
            Long eid2 = metadataId(m2, "data", "eid");
            if (!eid1.equals(eid2)) {
                return eid1.compareTo(eid2);
            }
            Long number1 = metadataId(m1, "revision", "number");
            Long number2 = metadataId(m2, "revision", "number");
            return number1.compareTo(number2);
        });
        return providers;
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
        throw new RuntimeException(String.format("Identifier %s exists in Map %s but is of type %s", identifier, map, id.getClass().getName()));
    }


}
