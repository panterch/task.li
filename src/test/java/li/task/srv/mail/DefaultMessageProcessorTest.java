/**
 * 
 */
package li.task.srv.mail;

import static org.junit.Assert.assertEquals;

import java.io.File;

import li.task.srv.model.InMemoryTaskListSrv;
import li.task.srv.model.TaskList;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

/**
 * @author seb
 * 
 */
public class DefaultMessageProcessorTest {
	
	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	private DefaultMessageProcessor instance;

	@Test
	public void shouldProcessSimpleMaildir() throws Exception {
		File unpackDir = tempFolder.newFolder();
		ClassPathResource maildirArchive = new ClassPathResource(
				"maildir-with-one-taskmail.tar");
		String archive = maildirArchive.getFile().getAbsolutePath();
		Runtime.getRuntime().exec("tar xvf " + archive + " -C " + unpackDir.getAbsolutePath());
		File maildir = new File(unpackDir, "Maildir");
		MaildirScanner maildirScanner = new MaildirScanner();
		maildirScanner.setMaildir(maildir);
		this.instance = new DefaultMessageProcessor();
		this.instance.setMaildirScanner(maildirScanner);
		this.instance.setTaskListSrv(new InMemoryTaskListSrv());
		TaskList taskList = this.instance.processMessage(null, null);
		assertEquals(3, taskList.getTasks().size());
	}
	
	


}
