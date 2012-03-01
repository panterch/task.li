package li.task.srv.integrationtest;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.io.File;

import li.task.srv.mail.DelegatingMessageProcessor;
import li.task.srv.mail.MaildirScanner;
import li.task.srv.mail.MockTransport;
import li.task.srv.model.FsTaskListSrv;
import li.task.srv.model.TaskList;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/applicationContext.xml"})
public class IncomingMessageProcessorTest {

	/** the service under test */
	@Autowired DelegatingMessageProcessor incomingMessageProcessor;

	/** the tasklist service, as used on production. Needs customization for tests */
	@Autowired FsTaskListSrv taskListSrv;
	/** the maildir scanner service, as used on production. Needs customization for tests */
	@Autowired MaildirScanner maildirScanner;
	/** per test work dir */
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	
	@Before
	public void setup() throws Exception {
		// customize fs tasklist service to work in temporary directory
		taskListSrv.setDir(folder.newFolder());
	}

	@Test
	public void shouldParseSimpleMaildirFixture() throws Exception {
		prepareMaildirFixture("simple-maildir.tar");		
		TaskList taskList = incomingMessageProcessor.processMessage(null);
		assertNotNull(taskList);
		assertNotNull(taskListSrv.getTaskList(taskList.getId()));
		assertEquals("Test Mail", taskList.getName());
		assertEquals("root <root@task.panter.ch>", taskList.getInitiator());
		assertThat(taskList.getCollaborators(), hasItem("jetty@task.panter.ch"));
		assertNotNull(MockTransport.getLastMessage());
	}
	
	@Test
	public void shouldParseMaildirFixtureWithOneTasklist() throws Exception {
		prepareMaildirFixture("maildir-with-one-taskmail.tar");		
		TaskList taskList = incomingMessageProcessor.processMessage(null);
		assertNotNull(taskList);
		assertEquals(3, taskList.getTasks().size());
	}
	
	private void prepareMaildirFixture(String name) throws Exception {
		String unpackDir = folder.newFolder().getAbsolutePath();
		ClassPathResource maildirArchive = new ClassPathResource(name);
		String archive = maildirArchive.getFile().getAbsolutePath();
		Runtime.getRuntime().exec("tar xvf " + archive + " -C " + unpackDir);
		maildirScanner.setMaildir(new File(unpackDir, "Maildir"));
		
	}
}
