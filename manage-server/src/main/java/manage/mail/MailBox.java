package manage.mail;

import manage.push.Delta;

import javax.mail.MessagingException;
import java.io.IOException;
import java.util.List;

public interface MailBox {

    void sendDeltaPushMail(List<Delta> realDeltas) throws IOException, MessagingException;

}
