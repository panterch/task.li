package li.task.srv.mail;

import javax.mail.Message;

import li.task.srv.model.TaskList;

public interface MessageProcessor {
	public abstract TaskList processMessage(TaskList taskList, Message message) throws Exception;
}