/*
 * JavaMaildir - a JavaMail service provider for Maildir mailboxes.
 * Copyright (C) 2002-2006 Alexander Zhukov
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package net.ukrpost.storage.maildir;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetHeaders;
import javax.mail.*;
import javax.activation.DataHandler;
import java.io.InputStream;
import java.util.Date;

public class ReadOnlyMessage extends MimeMessage {
    public ReadOnlyMessage(Session session) {
        super(session);
    }

    public ReadOnlyMessage(Session session, InputStream inputstream) throws MessagingException {
        super(session, inputstream);
    }

    public ReadOnlyMessage(MimeMessage mimemessage) throws MessagingException {
        super(mimemessage);
    }

    protected ReadOnlyMessage(Folder folder, int i) {
        super(folder, i);
    }

    protected ReadOnlyMessage(Folder folder, InputStream inputstream, int i) throws MessagingException {
        super(folder, inputstream, i);
    }

    protected ReadOnlyMessage(Folder folder, InternetHeaders internetheaders, byte[] abyte0, int i) throws MessagingException {
        super(folder, internetheaders, abyte0, i);
    }

    public void addHeaderLine(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setFrom(Address address) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setFrom() throws MessagingException {
        throw new IllegalWriteException();
    }

    public void addFrom(Address aaddress[]) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setSender(Address address) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setRecipients(Message.RecipientType recipienttype, Address aaddress[]) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setRecipients(Message.RecipientType recipienttype, String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void addRecipients(Message.RecipientType recipienttype, Address aaddress[]) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void addRecipients(Message.RecipientType recipienttype, String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setReplyTo(Address aaddress[]) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setSubject(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setSubject(String s, String s1) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setSentDate(Date date) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setDisposition(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setContentID(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setContentMD5(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setDescription(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setDescription(String s, String s1) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setContentLanguage(String as[]) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setFileName(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setDataHandler(DataHandler datahandler) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setContent(Object obj, String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setContent(Multipart multipart) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setText(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setText(String s, String s1) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setHeader(String s, String s1) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void addHeader(String s, String s1) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void removeHeader(String s) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void setRecipient(Message.RecipientType recipienttype, Address address) throws MessagingException {
        throw new IllegalWriteException();
    }

    public void addRecipient(Message.RecipientType recipienttype, Address address) throws MessagingException {
        throw new IllegalWriteException();
    }

/*
    public void saveChanges() throws MessagingException {
        throw new RuntimeException("immutable");
    }
    protected void updateHeaders() throws MessagingException {
        throw new RuntimeException("immutable");
    }
*/

}
