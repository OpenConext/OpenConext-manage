package manage.control;

import io.restassured.common.mapper.TypeRef;
import manage.AbstractIntegrationTest;
import manage.model.EntityType;
import manage.model.MetaData;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StepUpImportControllerTest extends AbstractIntegrationTest {

    @Test
    void importStepUpServiceProviders() throws IOException {
        String middlewareConfigJSON = IOUtils.toString(
            new ClassPathResource("stepup/middleware-config.json").getInputStream(), Charset.defaultCharset());
        List<MetaData> metaDataList = given()
            .auth()
            .preemptive()
            .basic("sysadmin", "secret")
            .body(middlewareConfigJSON)
            .header("Content-type", "application/json")
            .when()
            .post("/manage/api/internal/stepup/import/sfo")
            .as(new TypeRef<>() {
            });
        assertEquals(23, metaDataList.size());
        List<MetaData> metaDataFromDB = metaDataRepository.findAllByType(EntityType.SFO.getType());
        assertEquals(23, metaDataFromDB.size());
    }

    @Test
    void importStepUpInstitution() throws IOException {
        String middlewareInstitutionJSON = IOUtils.toString(
            new ClassPathResource("stepup/middleware-institution.json").getInputStream(), Charset.defaultCharset());
        List<MetaData> metaDataList = given()
            .auth()
            .preemptive()
            .basic("sysadmin", "secret")
            .body(middlewareInstitutionJSON)
            .header("Content-type", "application/json")
            .when()
            .post("/manage/api/internal/stepup/import/institution")
            .as(new TypeRef<>() {
            });
        assertEquals(15, metaDataList.size());
        List<MetaData> metaDataFromDB = metaDataRepository.findAllByType(EntityType.STEPUP.getType());
        assertEquals(15, metaDataFromDB.size());
    }
}
