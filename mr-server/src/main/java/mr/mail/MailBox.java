package mr.mail;

import com.fasterxml.jackson.core.JsonProcessingException;
import mr.push.Delta;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface MailBox {

    void sendDeltaPushMail(List<Delta> realDeltas) throws IOException, MessagingException;

}
