package li.task.srv.spring;

import static org.junit.Assert.*;
import li.task.srv.mail.DefaultMessageProcessor;
import li.task.srv.mail.MessageProcessor;
import li.task.srv.model.FsTaskListSrv;
import li.task.srv.model.TaskList;
import li.task.srv.model.TaskListSrv;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.sun.corba.se.impl.protocol.giopmsgheaders.Message;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"classpath:/applicationContext.xml"})
public class ProductionContextTest {

	@Autowired TaskListSrv taskListSrv;
	@Autowired MessageProcessor messageProcessor;
	
	@Test
	public void shouldWireTaskListSrv() {
		assertNotNull(taskListSrv);
		assertEquals(FsTaskListSrv.class, taskListSrv.getClass());
		assertNotNull(((FsTaskListSrv)taskListSrv).getDir());
	}

	@Test
	public void shouldWireMessageProcessor() {
		assertNotNull(messageProcessor);
		assertEquals(DefaultMessageProcessor.class, messageProcessor.getClass());
		assertNotNull(((DefaultMessageProcessor)messageProcessor).getTaskListSrv());
	}
}
