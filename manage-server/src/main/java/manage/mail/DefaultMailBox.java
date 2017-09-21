package manage.mail;

import com.fasterxml.jackson.databind.ObjectMapper;
import manage.push.Delta;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultMailBox implements MailBox {

    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private ObjectMapper objectMapper;

    private final String baseUrl;
    private final String to;
    private final String from;

    public DefaultMailBox(String baseUrl, String to, String from) {
        this.baseUrl = baseUrl;
        this.to = to;
        this.from = from;
    }

    @Override
    public void sendDeltaPushMail(List<Delta> realDeltas) throws IOException, MessagingException {
        Map<String, String> variables = new HashMap<>();
        variables.put("@@to@@", to);
        variables.put("@@differences@@", "<pre>" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(realDeltas) + "</pre>");
        variables.put("@@base_url@@", baseUrl);
        sendMail("mail/push_deltas.html", "Manage Push Delta", variables);
    }

    private void sendMail(String templateName, String subject, Map<String, String> variables) throws MessagingException, IOException {
        String html = IOUtils.toString(new ClassPathResource(templateName).getInputStream(), Charset.defaultCharset());
        for (Map.Entry<String, String> var : variables.entrySet()) {
            String value = var.getValue();
            value = value.replaceAll("\\$", "\\\\\\$");
            html = html.replaceAll(var.getKey(), value);
        }
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);
        helper.setSubject(subject);
        helper.setTo(to);
        setText(html, helper);
        helper.setFrom(from);
        doSendMail(message);
    }

    protected void setText(String html, MimeMessageHelper helper) throws MessagingException {
        helper.setText(html, true);
    }

    protected void doSendMail(MimeMessage message) {
        new Thread(() -> mailSender.send(message)).start();
    }

}
