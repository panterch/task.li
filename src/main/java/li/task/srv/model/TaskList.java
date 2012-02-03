/**
 * 
 */
package li.task.srv.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author seb
 *
 */
public class TaskList {
	
	private int id;
	private List<Task> tasks = new ArrayList<Task>();
	
	public TaskList() {
	}
	
	public TaskList(int id) {
		this.id = id;
	}

	public void addTask(Task task) {
		this.tasks.add(task);
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

}
