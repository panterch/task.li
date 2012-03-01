package li.task.srv.mail;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import li.task.srv.model.Task;
import li.task.srv.model.TaskList;

public class IncomingMessageResponder implements MessageProcessor {

	@Override
	public TaskList processMessage(TaskList taskList) throws Exception {
		StringBuilder content = new StringBuilder();
		content.append("Dear friendly user\n\n");
		content.append("We just took overyour tasklist "+taskList.getName()+":\n");
		for (Task task : taskList.getTasks()) {
			content.append("* "+task.getName()+"\n");
		}
		content.append("\n\n");
		content.append("The list is managed under:\n");
		content.append("http://www.task.li/tasklists/"+taskList.getId());
		
		// FIXME: we should share the session over all instances
		Session session = Session.getDefaultInstance(new Properties(), null);

		// FIXME: we should reply to the mail, not generating a new thread
		MimeMessage message = new MimeMessage(session);
		message.setFrom(new InternetAddress("task@task.li"));
		
		message.addRecipient(Message.RecipientType.TO, new InternetAddress(
				taskList.getInitiator()));
		
		message.setSubject("task.li is in charge");
		message.setText(content.toString());

		// Send message
		Transport.send(message);
		
		return taskList;
	}

}
