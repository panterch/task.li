/**
 * 
 */
package li.task.srv.mail;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;


/**
 * @author seb
 *
 */
public class JavaMailDeSerializationTest {
	
	@Test
	public void testDeSerialization() throws Exception {
		File rfcMessage = new ClassPathResource("simple.RFC822").getFile();
		Session session = Session.getInstance(new Properties());
		MimeMessage message = new MimeMessage(session, new FileInputStream(rfcMessage));
		assertThat(message.getContent().toString(), startsWith("Hello"));
	}

}
