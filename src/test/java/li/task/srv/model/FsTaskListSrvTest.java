package li.task.srv.model;

import static org.junit.Assert.*;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.hamcrest.Matchers.*;

public class FsTaskListSrvTest {
	
	@Rule
    public TemporaryFolder folder = new TemporaryFolder();
	private FsTaskListSrv instance = null;
	private File workDir = null;

	@Before
	public void setUp() throws Exception {
		this.workDir  = folder.getRoot();
		this.instance = new FsTaskListSrv(workDir);
	}

	@Test
	public void shouldSetDirectory() {
		assertEquals(workDir, this.instance.getDir());
	}
	
	@Test
	public void shouldWriteFile() throws Exception {
		TaskList tasklist = this.instance.storeTaskList(new TaskList());
		File file = new File(folder.getRoot(), tasklist.getId() + ".ser" );
		assertTrue("Missing file "+file.getPath(), file.exists());
	}
	
	@Test
	public void shouldOverwriteFile() throws Exception {
		new File(folder.getRoot(), "overwrite.ser" ).createNewFile();
		this.instance.storeTaskList(new TaskList("overwrite"));
		assertThat((new File(workDir, "overwrite.ser").length()), greaterThan(1L));
	}
	
	@Test
	public void shouldStoreAndReadComplexTaskList() throws Exception {
		TaskList taskList = new TaskList();
		taskList.addTask(new Task("Test Task"));
		taskList = this.instance.storeTaskList(taskList);
		taskList = this.instance.getTaskList(taskList.getId());
		assertEquals(1, taskList.getTasks().size());
		assertEquals("Test Task", taskList.getTasks().get(0).getName());
	}

}
