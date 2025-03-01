package manage.control;

import io.restassured.common.mapper.TypeRef;
import manage.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PdPControllerTest extends AbstractIntegrationTest {

    @Test
    public void policiesWithMissingPolicyEnforcementDecisionRequired() {
        List<Map<String, Object>> metaDataList = given()
                .when()
                .auth()
                .preemptive()
                .basic("sp-portal", "secret")
                .get("manage/api/internal/pdp/missing-enforcements")
                .as(new TypeRef<>() {
                });
        assertEquals(2, metaDataList.size());
        metaDataList.forEach(metaData -> assertEquals("policy",metaData.get("type")));
    }
}