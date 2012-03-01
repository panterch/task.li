/**
 * 
 */
package li.task.srv.mail;

import java.io.File;

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
public class DefaultMessageProcessor implements MessageProcessor {
	
	private TaskListSrv taskListSrv;
	private MaildirScanner maildirScanner;
	private BulletPointExtractor bulletPointExtractor = new BulletPointExtractor();
	final Logger logger = LoggerFactory.getLogger(DefaultMessageProcessor.class);
	

	public TaskList processMessage(TaskList taskList, Message msg) throws Exception {
		logger.info("calling maildir scanner");
		taskList = maildirScanner.processMessage(null, null);
		msg = taskList.getMessage();
		logger.info("calling bullet point extractor");
		taskList = bulletPointExtractor.processMessage(taskList, msg);
		logger.info("storing tasklist");
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


	public MaildirScanner getMaildirScanner() {
		return maildirScanner;
	}


	public void setMaildirScanner(MaildirScanner maildirScanner) {
		this.maildirScanner = maildirScanner;
	}

}
