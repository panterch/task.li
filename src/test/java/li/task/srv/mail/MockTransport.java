package li.task.srv.mail;

import javax.mail.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link Transport} for unit testing. This needs
 * property file overrides in a META-INF on the classpath in order to work; this
 * is in src/test/resources . See
 * http://sujitpal.blogspot.com/2006/12/mock-objects-for-javamail-unit-tests.html
 * for more information. This implementation
 * stores the last message sent and adds a getter for retrieval and
 * verification.
 * 
 * @author bartas
 */
public class MockTransport extends Transport {

	private static final Logger logger = LoggerFactory
			.getLogger(MockTransport.class);

	private static Message lastMessage;

	public MockTransport(Session session, URLName urlName) {
		super(session, urlName);
		logger.warn("constructed MockTransport instance - javamail is mocked");
	}

	/**
	 * Stores the message to send in this instance.
	 * 
	 * @see javax.mail.Transport#sendMessage(javax.mail.Message,
	 *      javax.mail.Address[])
	 */
	@Override
	public void sendMessage(Message arg0, Address[] arg1)
			throws MessagingException {

		String subject = arg0.getSubject();
		StringBuffer addresses = new StringBuffer("[");
		for (int i = 0; i < arg1.length; i++) {
			addresses.append(arg1[i]);
			if (i < arg1.length - 1)
				addresses.append(",");
		}
		addresses.append("]");
		logger.debug("sendMessage(\"{}\",{})", subject, addresses.toString());
		lastMessage = arg0;
	}

	@Override
	public void connect() throws MessagingException {
		logger.debug("connect()");
	}

	@Override
	public void connect(String arg0, int arg1, String arg2, String arg3)
			throws MessagingException {
		logger.debug("connect({},{},{},{})", new Object[] { arg0, arg1, arg2,
				arg3 });
	}

	@Override
	public void connect(String arg0, String arg1, String arg2)
			throws MessagingException {
		logger.debug("connect({},{},{})", new Object[] { arg0, arg1, arg2 });
	}

	@Override
	public void connect(String arg0, String arg1) throws MessagingException {
		logger.debug("connect({},{})", arg0, arg1);
	}

	@Override
	public synchronized void close() throws MessagingException {
		logger.debug("close()");
	}

	public static Message getLastMessage() {
		return lastMessage;
	}

}
