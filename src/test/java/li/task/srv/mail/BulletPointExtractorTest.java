/**
 * 
 */
package li.task.srv.mail;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import li.task.srv.model.TaskList;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * @author seb
 * 
 */
public class BulletPointExtractorTest {
	
	private Session session = Session.getInstance(new Properties());
	private BulletPointExtractor instance;
	private TaskList taskList;
	
	@Before
	public void setup() {
		instance = new BulletPointExtractor();
		taskList = new TaskList();
	}


	@Test
	public void shouldExtractTasksFromFixture() throws Exception {
		TaskList taskList = tasklistFromFixture("tasklist.RFC822");
		taskList = instance.processMessage(taskList, taskList.getMessage());
		assertEquals(3, taskList.getTasks().size());
	}
	
	@Test
	public void shouldExtractTaskWithAsterisk() throws Exception {
		instance.addTask(taskList, " * Go shopping");
		assertEquals("Go shopping", taskList.getTasks().get(0).getName());
	}
	
	private TaskList tasklistFromFixture(String name) throws Exception {
		File rfcMessage = new ClassPathResource(name).getFile();
		MimeMessage message = new MimeMessage(session, new FileInputStream(
				rfcMessage));
		TaskList taskList = new TaskList();
		taskList.setMessage(message);
		return taskList;
	}

}
