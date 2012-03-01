package li.task.srv.mail.poc;

import static org.junit.Assert.*;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import li.task.srv.mail.MockTransport;

import org.junit.Test;

public class TransportMockTest {

	@Test
	public void test() throws AddressException, MessagingException {

		// Get session
		Session session = Session.getDefaultInstance(new Properties(), null);

		// Define message
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress("sender@example.com"));
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(
				"receiver@example.com"));
		message.setSubject("Hello JavaMail");
		message.setText("Welcome to JavaMail");

		// Send message
		Transport.send(message);
		
		assertNotNull(MockTransport.getLastMessage());
	}

}
