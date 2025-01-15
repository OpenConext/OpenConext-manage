package manage.control;

import manage.model.MetaData;
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

import java.util.List;

@RestController
@SuppressWarnings("unchecked")
public class PdPController {

    private final String decideUrl;
    private final RestTemplate pdpRestTemplate;
    private final MetaDataRepository metaDataRepository;
    private final HttpHeaders headers;

    public PdPController(@Value("${push.pdp.decide_url}") String decideUrl,
                         @Value("${push.pdp.user}") String pdpUser,
                         @Value("${push.pdp.password}") String pdpPassword,
                         MetaDataRepository metaDataRepository) {
        this.decideUrl = decideUrl;
        this.metaDataRepository = metaDataRepository;
        this.pdpRestTemplate = new RestTemplate();
        this.pdpRestTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(pdpUser, pdpPassword));
        this.headers = new HttpHeaders();
        this.headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping(value = "/client/pdp/decide")
    public String decideManage(@RequestBody String payload) {
        HttpEntity<?> requestEntity = new HttpEntity<>(payload, headers);
        return pdpRestTemplate.exchange(this.decideUrl, HttpMethod.POST, requestEntity, String.class).getBody();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping(value = "/client/pdp/missing-enforcements")
    public List<MetaData> policiesWithMissingPolicyEnforcementDecisionRequired() {
        return metaDataRepository.policiesWithMissingPolicyEnforcementDecisionRequired();
    }

}
