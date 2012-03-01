/**
 * 
 */
package li.task.srv.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.mail.Message;

import org.springframework.test.context.transaction.TransactionConfiguration;

/**
 * @author seb
 *
 */
public class TaskList implements Serializable{
	
	private static final long serialVersionUID = 6786660919869427134L;
	
	private String id = null;
	private String name;
	private String initiator;
	private List<String> collaborators = new ArrayList<String>();
	private List<Task> tasks = new ArrayList<Task>();
	
	private transient Message message;
	
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

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public List<Task> getTasks() {
		return tasks;
	}

	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getInitiator() {
		return initiator;
	}

	public void setInitiator(String initiator) {
		this.initiator = initiator;
	}

	public List<String> getCollaborators() {
		return collaborators;
	}

	public void setCollaborators(List<String> collaborators) {
		this.collaborators = collaborators;
	}
	
	public void addCollaborator(String collaborator) {
		this.collaborators.add(collaborator);
	}

}
