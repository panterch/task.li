package li.task.srv.mail;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import li.task.srv.mail.MaildirScanner;
import li.task.srv.model.TaskList;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

public class MaildirScannerTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();
	private File maildir;
	private MaildirScanner instance;

	@Before
	public void setUp() throws Exception {
		File unpackDir = tempFolder.newFolder();
		ClassPathResource maildirArchive = new ClassPathResource(
				"simple-maildir.tar");
		String archive = maildirArchive.getFile().getAbsolutePath();
		Runtime.getRuntime().exec("tar xvf " + archive + " -C " + unpackDir.getAbsolutePath());
		this.maildir = new File(unpackDir, "Maildir");
		this.instance = new MaildirScanner();
	}
	
	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void shouldExtractFixtureArchive() throws Exception {
		assertTrue(this.maildir.isDirectory());
	}

	
	@Test
	public void shouldCreateNewTodolistHoldingTheMessage() throws Exception {
		instance.setMaildir(this.maildir);
		TaskList taskList = instance.processMessage(null, null);
		assertNotNull(taskList.getMessage());
	}
	

}
