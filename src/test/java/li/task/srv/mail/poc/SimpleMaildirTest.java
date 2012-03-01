package li.task.srv.mail.poc;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.core.io.ClassPathResource;

public class SimpleMaildirTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	private String workDir = null;
	private Store store;

	@Before
	public void setUp() throws Exception {
		this.workDir = folder.newFolder().getAbsolutePath();
		ClassPathResource maildirArchive = new ClassPathResource(
				"simple-maildir.tar");
		String archive = maildirArchive.getFile().getAbsolutePath();
		Runtime.getRuntime().exec("tar xvf " + archive + " -C " + workDir);
		Session session = Session.getInstance(new Properties());
		String url = "maildir:///" + new File(workDir, "Maildir").getAbsolutePath();
		store = session.getStore(new URLName(url));
	}
	
	@After
	public void tearDown() throws Exception {
		store.close();
	}

	@Test
	public void shouldExtractFixtureArchive() throws Exception {
		assertTrue(new File(workDir, "Maildir").isDirectory());
	}

	@Test
	public void shouldReadSubjectWithJavamaildir() throws Exception {
		Folder inbox = store.getFolder("inbox");
		inbox.open(Folder.READ_WRITE);
		Message m = inbox.getMessage(1);
		assertEquals("Test Mail", m.getSubject());
	}

	@Test
	public void shouldReadContentWithJavamaildir() throws Exception {
		Folder inbox = store.getFolder("inbox");
		inbox.open(Folder.READ_WRITE);
		Message m = inbox.getMessage(1);
		assertThat(m.getContent().toString(), containsString("Hello, world"));
	}
	
	@Test
	public void shouldSetFlags() throws Exception {
		Folder inbox = store.getFolder("inbox");
		assertEquals(1, inbox.getNewMessageCount());
		inbox.open(Folder.READ_WRITE);
		Message m = inbox.getMessage(1);

		m.setFlag(Flags.Flag.RECENT, false);
		assertEquals(0, inbox.getNewMessageCount());
		
		inbox.close(true);
		String[] mailFiles = new File(workDir, "Maildir/cur").list();
		assertThat(mailFiles[0], containsString(",S"));
	}

	@Test
	public void shouldExpungeMessages() throws Exception {
		Folder inbox = store.getFolder("inbox");
		inbox.open(Folder.READ_WRITE);
		Message m = inbox.getMessage(1);

		m.setFlag(Flags.Flag.DELETED, true);
		
		inbox.close(true);
		String[] mailFiles = new File(workDir, "Maildir/cur").list();
		assertEquals(0, mailFiles.length);
	}


}
