package li.task.srv.resource;

import static org.junit.Assert.*;

import javax.ws.rs.core.MediaType;

import li.task.srv.model.InMemoryTaskListSrv;
import li.task.srv.model.Task;
import li.task.srv.model.TaskList;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import com.riffpie.common.testing.SpringAwareGrizzlyTestContainerFactory;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.spi.spring.container.servlet.SpringServlet;
import com.sun.jersey.test.framework.JerseyTest;
import com.sun.jersey.test.framework.WebAppDescriptor;
import com.sun.jersey.test.framework.spi.container.TestContainerException;
import com.sun.jersey.test.framework.spi.container.TestContainerFactory;

public class TaskListResourceIntegrationTest extends JerseyTest {

	@Autowired
	InMemoryTaskListSrv inMemoryTaskListSrv;

	protected TestContainerFactory getTestContainerFactory()
			throws TestContainerException {
		return new SpringAwareGrizzlyTestContainerFactory(this);
	}

	public TaskListResourceIntegrationTest() throws Exception {
		super(new WebAppDescriptor.Builder("li.task.srv.resource")
				.contextPath("/")
				.contextParam("contextConfigLocation", "classpath:testContext.xml")
				.initParam("com.sun.jersey.api.json.POJOMappingFeature", "true")
				.servletClass(SpringServlet.class)
				.contextListenerClass(ContextLoaderListener.class).build());
	}

	/*
	 * the integration test uses its own server. it is important that server and
	 * this resource use the same application context. this is checked here by
	 * comparing the object hash printed by the configuration request.
	 */
	@Test
	public void shouldUseSameInMemoryTaskListSrvAsInjected() throws Exception {
		WebResource webResource = resource();
		String responseMsg = webResource.path("_configuration")
				.accept(MediaType.TEXT_PLAIN_TYPE).get(String.class);
		assertContains(inMemoryTaskListSrv.toString(), responseMsg);
	}

	@Test
	public void shouldThrowFileNotFoundWhenNavigatingToUnknownPath()
			throws Exception {
		WebResource webResource = resource();
		ClientResponse responseMsg = webResource.path("unknown")
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.get(ClientResponse.class);
		assertEquals(404, responseMsg.getStatus());
	}

	@Test
	public void shouldDeliverJSONRepresentationOfTasklist() throws Exception {
		TaskList taskList = new TaskList("test-task");
		Task task = new Task("Learn to swim");
		taskList.addTask(task);
		this.inMemoryTaskListSrv.storeTaskList(taskList);
		WebResource webResource = resource();
		String responseMsg = webResource.path("tasklists/test-task")
				.accept(MediaType.APPLICATION_JSON_TYPE).get(String.class);
		assertContains("swim", responseMsg);
	}

	@Test
	public void shouldDeliver404whenTaskNotAvailable() throws Exception {
		WebResource webResource = resource();
		ClientResponse responseMsg = webResource.path("tasklists/does-not-exist")
				.accept(MediaType.APPLICATION_JSON_TYPE)
				.get(ClientResponse.class);
		assertEquals(404, responseMsg.getStatus());
	}
	
	protected void assertContains(String expected, String actual) {
		assertTrue("Invalid response msg:\n" + actual + "\nexpected:\n "
				+ expected, actual.contains(expected));
	}

}
