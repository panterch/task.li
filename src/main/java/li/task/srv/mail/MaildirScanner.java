/**
 * 
 */
package li.task.srv.mail;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;

import li.task.srv.model.TaskList;

/**
 * @author seb
 *
 */
public class MaildirScanner implements MessageProcessor {
	
	protected Session session = Session.getInstance(new Properties());
	protected File maildir; 
	
	public File getMaildir() {
		return maildir;
	}

	public void setMaildir(File maildir) {
		this.maildir = maildir;
	}
	
	public TaskList processMessage(TaskList taskList, Message msg) throws Exception {
		if (!maildir.isDirectory()) {
			throw new IllegalArgumentException("must submit a directory: "+maildir.getAbsolutePath());
		}
		if (!maildir.canRead()) {
			throw new IllegalArgumentException("must be readable: "+maildir.getAbsolutePath());
		}

		String url = "maildir:///" + maildir.getAbsolutePath();
		Store store = session.getStore(new URLName(url));
		
		Folder inbox = store.getFolder("inbox");
		inbox.open(Folder.READ_WRITE);
		if (!inbox.hasNewMessages()) {
			throw new Exception("there are no new messages to process in inbox");
		}
		msg = inbox.getMessage(1);
		
		// parse the message so that the inbox may be closed
		msg.getSubject();
		msg.getContent();
		msg.setFlag(Flags.Flag.DELETED, true);
		inbox.close(true);
		store.close();
		
		taskList = new TaskList();
		taskList.setMessage(msg);
		return taskList;
	}

}
