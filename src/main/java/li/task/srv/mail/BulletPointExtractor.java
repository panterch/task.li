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
public class BulletPointExtractor implements MessageProcessor {
	
	final Logger logger = LoggerFactory.getLogger(BulletPointExtractor.class);

	public TaskList processMessage(TaskList taskList, Message msg) throws Exception {	
		String content = msg.getContent().toString();
		logger.info("Processing message content:\n"+content);
		BufferedReader r = new BufferedReader(new StringReader(content));
		
		String line;
		while ((line = r.readLine()) != null) {
		  line = line.trim();
		  if (line.startsWith("*") || line.startsWith("-")) {
			  logger.info("Detected taks: "+line);
			  Task t = new Task();
			  t.setName(line);
			  taskList.addTask(t);
		  }
		}

		r.close();
		return taskList;
	}

}
