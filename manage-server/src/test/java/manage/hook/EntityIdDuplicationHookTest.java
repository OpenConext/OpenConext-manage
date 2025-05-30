package manage.hook;

import manage.AbstractIntegrationTest;
import manage.api.APIUser;
import manage.model.EntityType;
import manage.model.MetaData;
import org.everit.json.schema.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.StringUtils;

import java.util.*;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;

class EntityIdDuplicationHookTest extends AbstractIntegrationTest {

    private EntityIdDuplicationHook entityIdDuplicationHook;

    private final APIUser apiUser = new APIUser("test", emptyList());

    @BeforeEach
    public void before() throws Exception {
        super.before();
        entityIdDuplicationHook = new EntityIdDuplicationHook(metaDataAutoConfiguration, metaDataRepository);
    }

    @Test
    void appliesForMetaData() {
        Arrays.stream(EntityType.values())
            .forEach(entityType -> assertTrue(entityIdDuplicationHook.appliesForMetaData(new MetaData(entityType.getType(), Map.of()))));
    }

    @Test
    void prePost() {
        List.of(EntityType.RP, EntityType.SP, EntityType.SRAM).forEach(entityType -> {
            try {
                this.entityIdDuplicationHook.prePost(metaData(null, "http://mock-sp", entityType), apiUser);
                fail("Expected ValidationException");
            } catch (ValidationException e) {
                assertEquals("#: Duplicate entityid http://mock-sp in collection saml20_sp with id 3",
                    e.getMessage());
            }
        });
    }

    @Test
    void prePutValid() {
        this.entityIdDuplicationHook.prePut(null, metaData("3", "http://mock-sp", EntityType.SP), apiUser);
    }

    @Test
    void prePutInValid() {
        Arrays.stream(EntityType.values())
            .forEach(entityType -> {
                List<MetaData> allByType = metaDataRepository.findAllByType(entityType.getType());
                MetaData metaData = allByType.getFirst();
                //This is allowed
                this.entityIdDuplicationHook.prePut(null, metaData, apiUser);
                //Set new unique id
                ReflectionTestUtils.setField(metaData, "id", UUID.randomUUID().toString());
                assertThrows(ValidationException.class, () ->
                    this.entityIdDuplicationHook.prePut(null, metaData, apiUser));
                ReflectionTestUtils.setField(metaData, "id", null);
                assertThrows(ValidationException.class, () ->
                    this.entityIdDuplicationHook.prePost(metaData, apiUser));
            });

    }


    private MetaData metaData(String id, String entityid, EntityType entityType) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityid", entityid);
        MetaData metaData = new MetaData(entityType.getType(), data);
        if (StringUtils.hasText(id)) {
            metaData.initial(id, "system", 1L);
        }
        return metaData;
    }
}
