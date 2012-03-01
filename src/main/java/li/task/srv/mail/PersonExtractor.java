/**
 * 
 */
package li.task.srv.mail;

import javax.mail.Address;
import javax.mail.Message;

import li.task.srv.model.TaskList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author seb
 * 
 */
public class PersonExtractor extends BaseMessageProcessor implements
		MessageProcessor {

	final Logger logger = LoggerFactory.getLogger(PersonExtractor.class);

	@Override
	protected TaskList processMessage(TaskList taskList, Message msg)
			throws Exception {
		
		for (Address addr : msg.getFrom()) {
			taskList.setInitiator(addr.toString());
		}
		for (Address addr : msg.getAllRecipients()) {
			if (addr.toString().matches("@task.li")) { continue; }
			taskList.addCollaborator(addr.toString());
		}
		
		return taskList;
	}

}
