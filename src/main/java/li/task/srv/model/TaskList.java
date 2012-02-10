/**
 * 
 */
package li.task.srv.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author seb
 *
 */
public class TaskList implements Serializable{
	
	private static final long serialVersionUID = 6786660919869427134L;
	
	private String id = null;
	private List<Task> tasks = new ArrayList<Task>();
	
	public TaskList() {
		this.id = UUID.randomUUID().toString();
	}
	
	public TaskList(String id) {
		this.id = id;
	}

	public void addTask(Task task) {
		this.tasks.add(task);
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

}
