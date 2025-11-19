package manage.control;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import manage.model.MetaData;
import manage.policies.PdpPolicyDefinition;
import manage.repository.MetaDataRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@SuppressWarnings("unchecked")
public class PdPController {

    private final String decideUrl;
    private final RestTemplate pdpRestTemplate;
    private final MetaDataRepository metaDataRepository;
    private final String parseUrl;
    private final ObjectMapper objectMapper;

    public PdPController(@Value("${push.pdp.decide_url}") String decideUrl,
                         @Value("${push.pdp.parse_url}") String parseUrl,
                         @Value("${push.pdp.user}") String pdpUser,
                         @Value("${push.pdp.password}") String pdpPassword,
                         MetaDataRepository metaDataRepository, ObjectMapper objectMapper) {
        this.decideUrl = decideUrl;
        this.parseUrl = parseUrl;
        this.metaDataRepository = metaDataRepository;
        this.pdpRestTemplate = RestTemplateIdiom.buildRestTemplate(null, pdpUser, pdpPassword);
        this.objectMapper = objectMapper;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/pdp/decide")
    public String decideManage(@RequestBody String payload) {
        HttpHeaders headers =  new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> requestEntity = new HttpEntity<>(payload, headers);
        return pdpRestTemplate.exchange(this.decideUrl, HttpMethod.POST, requestEntity, String.class).getBody();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'READ')")
    @GetMapping(value = {"/client/pdp/conflicts", "/internal/pdp/conflicts"})
    public Map<String, List<MetaData>> conflictingPolicies() {
        return metaDataRepository.conflictingPolicies();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/pdp/parse")
    public Map<String, String> xml(@RequestBody Map<String, Object> data) {
        MetaData metaData = new MetaData("policy", data);
        //Too prevent NullPointers
        metaData.initial(UUID.randomUUID().toString(), "system", 1L);
        PdpPolicyDefinition policyDefinition = new PdpPolicyDefinition(metaData);
        HttpHeaders headers =  new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> requestEntity = new HttpEntity<>(policyDefinition, headers);
        String res = pdpRestTemplate.exchange(this.parseUrl, HttpMethod.POST, requestEntity, String.class).getBody();
        return Map.of("xml", res);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/pdp/parse-json")
    public Map<String, Object> json(@RequestBody Map<String, Object> data) {
        MetaData metaData = new MetaData("policy", data);
        //Too prevent NullPointers
        metaData.initial(UUID.randomUUID().toString(), "system", 1L);
        PdpPolicyDefinition policyDefinition = new PdpPolicyDefinition(metaData);
        Map<String, Object> map = this.objectMapper.convertValue(policyDefinition, new TypeReference<>() {
        });
        map.entrySet().removeIf(entry -> {
            Object value = entry.getValue();
            if (value == null ||
                value instanceof String && ((String) value).isEmpty() ||
                value instanceof Collection && ((Collection<?>) value).isEmpty()) {
                return true;
            }
            return false;
        });
        return map;
    }
}
