package li.task.srv.model;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.hamcrest.Matchers.*;

public class InMemoryTaskListSrvTest {
	
	private InMemoryTaskListSrv instance = null;

	@Before
	public void setUp() throws Exception {
		this.instance = new InMemoryTaskListSrv();
	}

	@Test
	public void shouldSetupStore() {
		assertEquals(0, this.instance.getStore().size());
	}
	
	@Test
	public void shouldStoreAndReturn() throws Exception {
		TaskList tasklist = this.instance.storeTaskList(new TaskList());
		assertEquals(tasklist, this.instance.getTaskList(tasklist.getId()));
	}
	
	
}
