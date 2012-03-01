package li.task.srv.integrationtest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import li.task.srv.mail.DelegatingMessageProcessor;
import li.task.srv.mail.MessageProcessor;
import li.task.srv.model.FsTaskListSrv;
import li.task.srv.model.TaskListSrv;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/applicationContext.xml"})
public class ProductionContextTest {

	@Autowired TaskListSrv taskListSrv;
	@Autowired MessageProcessor incomingMessageProcessor;
	
	@Test
	public void shouldWireTaskListSrv() {
		assertNotNull(taskListSrv);
		assertEquals(FsTaskListSrv.class, taskListSrv.getClass());
		assertNotNull(((FsTaskListSrv)taskListSrv).getDir());
	}

	@Test
	public void shouldWireIncomingMessageProcessor() {
		assertNotNull(incomingMessageProcessor);
		assertEquals(DelegatingMessageProcessor.class, incomingMessageProcessor.getClass());
	}
}
