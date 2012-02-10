/**
 * 
 */
package li.task.srv.model;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Calendar;

/**
 * @author seb
 * 
 */
public class FsTaskListSrv implements TaskListSrv {

	private File dir = null;

	public FsTaskListSrv() {

	}

	public FsTaskListSrv(File dir) {
		if (!dir.isDirectory() || !dir.canWrite()) {
			throw new IllegalArgumentException("Can only work with writeable directories, not with "+dir.getPath());
		}
		this.dir = dir;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see li.task.srv.model.TaskListSrv#getTaskList(java.lang.String)
	 */
	@Override
	public TaskList getTaskList(String id) throws Exception {
		File file = buildFile(id);
		if (!file.exists()) {
			return null;
		}
		FileInputStream fis = new FileInputStream(file);
		return (TaskList) new ObjectInputStream(fis).readObject();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * li.task.srv.model.TaskListSrv#storeTaskList(li.task.srv.model.TaskList)
	 */
	@Override
	public TaskList storeTaskList(TaskList taskList) throws Exception {
		File file = buildFile(taskList.getId());
		FileOutputStream fos = new FileOutputStream(file);
		ObjectOutputStream out = new ObjectOutputStream(fos);
		out.writeObject(taskList);
		out.close();
		return taskList;
	}
	
	public File getDir() {
		return dir;
	}

	public void setDir(File dir) {
		this.dir = dir;
	}
	
	public File buildFile(String id) {
		return new File(this.dir, id + ".ser");
	}

}
