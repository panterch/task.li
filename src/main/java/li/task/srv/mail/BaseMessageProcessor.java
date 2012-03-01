package li.task.srv.mail;

import javax.mail.Message;

import li.task.srv.model.TaskList;

public abstract class BaseMessageProcessor implements MessageProcessor {

	@Override
	public TaskList processMessage(TaskList taskList) throws Exception {
		if (null != taskList) {
			return processMessage(taskList, taskList.getMessage());
		} else
			return processMessage(null, null);
	}

	protected abstract TaskList processMessage(TaskList taskList, Message message)  throws Exception;

}
