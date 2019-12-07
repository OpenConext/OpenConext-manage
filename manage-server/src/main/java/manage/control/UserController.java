package manage.control;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import manage.shibboleth.FederatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @GetMapping("/client/users/me")
    public FederatedUser me(FederatedUser federatedUser) {
        return federatedUser;
    }

    @GetMapping("/client/users/ping")
    public Map<String, String> ping() {
        return Collections.singletonMap("Ping", "Ok");
    }

    @DeleteMapping("/client/users/logout")
    public void logout(HttpServletRequest request) {
        request.getSession().invalidate();
        SecurityContextHolder.clearContext();
    }

    @PostMapping("/client/users/error")
    public void error(@RequestBody Map<String, Object> payload, FederatedUser federatedUser) throws
            JsonProcessingException, UnknownHostException {
        payload.put("dateTime", new SimpleDateFormat("yyyyy-mm-dd hh:mm:ss").format(new Date()));
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
