/**
 * 
 */
package li.task.srv.mail;

import java.io.BufferedReader;
import java.io.StringReader;

import javax.mail.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.task.srv.model.Task;
import li.task.srv.model.TaskList;

/**
 * @author seb
 * 
 */
public class BulletPointExtractor extends BaseMessageProcessor implements
		MessageProcessor {

	final Logger logger = LoggerFactory.getLogger(BulletPointExtractor.class);

	@Override
	protected TaskList processMessage(TaskList taskList, Message msg)
			throws Exception {
		String content = msg.getContent().toString();
		logger.info("Processing message content:\n" + content);
		BufferedReader r = new BufferedReader(new StringReader(content));

		String line;
		while ((line = r.readLine()) != null) {
			addTask(taskList, line);
		}

		r.close();
		return taskList;
	}

	protected void addTask(TaskList taskList, String line) {
		line = line.trim();
		if (line.startsWith("*") || line.startsWith("-")) {
			line = line.substring(1); // remove bullet point
			line = line.trim();
			logger.info("Detected taks: " + line);
			Task t = new Task();
			t.setName(line);
			taskList.addTask(t);
		}

	}

}
