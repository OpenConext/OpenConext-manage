package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import manage.shibboleth.FederatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@RestController
public class UserController {

    private static final Logger LOG = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${gui.disclaimer.background-color}")
    private String disclaimerBackgroundColor;

    @Value("${gui.disclaimer.content}")
    private String disclaimerContent;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/users/me")
    public FederatedUser me(FederatedUser federatedUser) throws JsonProcessingException {
        return federatedUser;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/client/users/ping")
    public Map<String, String> ping() {
        return Collections.singletonMap("Ping", "Ok");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/client/users/logout")
    public void logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/client/users/error")
    public void error(@RequestBody Map<String, Object> payload, FederatedUser federatedUser) throws
            JsonProcessingException, UnknownHostException {
        payload.put("dateTime", new SimpleDateFormat("yyyy-MM-dd hh:mm:ss").format(new Date()));
        payload.put("machine", InetAddress.getLocalHost().getHostName());
        payload.put("user", federatedUser);
        String msg = objectMapper.writeValueAsString(payload);
        LOG.error(msg, new IllegalArgumentException(msg));
    }

    @GetMapping("/client/users/disclaimer")
    public void disclaimer(HttpServletResponse response) throws IOException {
        response.setContentType("text/css");
        response.getWriter().write("body::after {background: " + disclaimerBackgroundColor + ";content: \"" +
                disclaimerContent + "\";}");
        response.getWriter().flush();

    }

}
