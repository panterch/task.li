/**
 * 
 */
package li.task.srv.mail;

import java.io.File;
import java.util.List;

import javax.mail.Message;

import li.task.srv.model.InMemoryTaskListSrv;
import li.task.srv.model.TaskList;
import li.task.srv.model.TaskListSrv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seb
 *
 */
public class DelegatingMessageProcessor implements MessageProcessor {
	
	private List<MessageProcessor> delegates;
	final Logger logger = LoggerFactory.getLogger(DelegatingMessageProcessor.class);
	
	public TaskList processMessage(TaskList taskList) throws Exception {
		for (MessageProcessor delegate : delegates) {
			logger.info("processing message using [{}]", delegate.getClass().getSimpleName());
			taskList = delegate.processMessage(taskList);
		}
		return taskList;
	}

	public List<MessageProcessor> getDelegates() {
		return delegates;
	}

	public void setDelegates(List<MessageProcessor> delegates) {
		this.delegates = delegates;
	}



}
