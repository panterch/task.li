/**
 * 
 */
package li.task.srv.mail;

import li.task.srv.model.TaskList;
import li.task.srv.model.TaskListSrv;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seb
 *
 */
public class StoringMessageProcessor implements MessageProcessor {
	
	private TaskListSrv taskListSrv;
	final Logger logger = LoggerFactory.getLogger(StoringMessageProcessor.class);
	

	public TaskList processMessage(TaskList taskList) throws Exception {
		taskListSrv.storeTaskList(taskList);
		logger.info("tasklist stored as {}", taskList.getId());
		return taskList;
	}


	public TaskListSrv getTaskListSrv() {
		return taskListSrv;
	}


	public void setTaskListSrv(TaskListSrv taskListSrv) {
		this.taskListSrv = taskListSrv;
	}

}
