package li.task.srv.model;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.hamcrest.Matchers.*;

public class TaskListTest {
	

	@Test
	public void shouldSetUniqueId() {
		assertEquals(36, new TaskList().getId().length());
	}

	
}
