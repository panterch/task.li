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

import net.ukrpost.utils.QuotaExceededException;
import net.ukrpost.utils.SharedFileInputStream;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.event.MessageChangedEvent;
import java.io.*;
import java.util.Date;
import java.util.Enumeration;

public class MaildirMessage extends ReadOnlyMessage implements Comparable {

    private MaildirFilename mfn;
    private boolean isparsed = false;
    private SharedFileInputStream parseStream;

    protected MaildirMessage(MaildirFolder folder, int msgnum) {
        super(folder, msgnum);
    }

    protected MaildirMessage(MaildirFolder folder, File f, int msgnum) {
        this(folder, f, new MaildirFilename(f), msgnum);
    }

    protected MaildirMessage(MaildirFolder folder, File f, MaildirFilename mfn, int msgnum) {
        super(folder, msgnum);

        isparsed = false;

        mfn.setSize(f.length());
        this.mfn = mfn;
    }

    public int getSize() {
        return (int) mfn.getSize();
    }

    protected void parse(InputStream is)
            throws MessagingException {

        if (!isparsed) {
//            updateFilename();
            super.parse(is);
            isparsed = true;
        }
    }

    public String getHeader(String name, String delim)
            throws MessagingException {
        parse();
        return super.getHeader(name, delim);
    }

    public String[] getHeader(String name)
            throws MessagingException {
        parse();
        return super.getHeader(name);
    }

    protected void parse()
            throws MessagingException {
        if (isparsed)
            return;

        if (parseStream != null)
            throw new RuntimeException("parseStream not null");

        long free = Runtime.getRuntime().freeMemory();
        boolean inmemory = (getFile().length() * 1.6 < free);

        InputStream in = null;
        try {
            if (inmemory) {
                in = new FileInputStream(getFile());
                parse(in);
            } else {
                parseStream = new SharedFileInputStream(getFile().toString());
                parse(parseStream);
            }
        } catch (FileNotFoundException fnfex) {
            closeQuietly(parseStream);
            parseStream = null;
            throw new MessagingException("file " + getFile() + " not found", fnfex);
        } catch (IOException e) {
            closeQuietly(parseStream);
            parseStream = null;
            throw new MessagingException("", e);
        } finally {
            if (inmemory) closeQuietly(in);
        }
    }

    private void closeQuietly(InputStream in) {
        if (in != null) try {
            in.close();
        } catch (IOException e) {
        }
    }

    public MaildirFilename getMaildirFilename() {
        return mfn;
    }

    protected File getFile() {
        return mfn.getFile();
    }

    public String toString() {
        return "" + getMessageNumber() + ":'" + mfn.toString() + '\'';
    }

    //duplicate method of Message.setMessageNumber() ?
    void setMsgNum(int mn) {
        msgnum = mn;
    }

    public void writeTo(OutputStream os)
            throws IOException, MessagingException {
        writeTo(os, null);
    }

    public void writeTo(OutputStream os, String as[])
            throws IOException, MessagingException {
        if (isparsed) {
            super.writeTo(os, as);
            return;
        }

        if (as != null) {
            parse();
            super.writeTo(os, as);
            return;
        }

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(getFile());
            final byte[] buff = new byte[8192];
            int i;
            while ((i = fis.read(buff)) > 0)
                os.write(buff, 0, i);
        } catch (QuotaExceededException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MessagingException("unable to retrieve message stream", ex);
        } finally {
            if (fis != null)
                try {
                    fis.close();
                } catch (Exception ex) {
                }
            if (os != null)
                try {
                    os.flush();
                } catch (Exception ex) {
                }
        }
    }

    public void setFlags(Flags f, boolean set)
            throws MessagingException {
        final Flags.Flag[] flags = f.getSystemFlags();
        boolean changed = false;

        for (int i = 0; i < flags.length; i++)
            changed = changed | (_setFlag(flags[i], set));

        if (!changed)
            return;

        ((MaildirFolder) getFolder()).localNotifyMessageChangedListeners(MessageChangedEvent.FLAGS_CHANGED, FlagChangedEvent.getEventType(f, set), this);
    }

    public void setFlag(Flags.Flag f, boolean set)
            throws MessagingException {

        if (!_setFlag(f, set))
            return;

        ((MaildirFolder) getFolder()).localNotifyMessageChangedListeners(MessageChangedEvent.FLAGS_CHANGED, FlagChangedEvent.getEventType(f, set), this);
    }

    private boolean _setFlag(Flags.Flag f, boolean set) {
        boolean changed;

        if (set) {
            changed = (!getFlags().contains(f));
            mfn.setFlag(f);
        } else {
            changed = (getFlags().contains(f));
            mfn.removeFlag(f);
        }

        return changed;
    }

	/**
	 * From JavaMail API doc: <i>Modifying any of the flags in this returned Flags
	 * object will not affect the flags of this message. Use setFlags() to do
	 * that.</i>
	 */
    public Flags getFlags() {
        return mfn.getFlags();
    }

    public boolean isSet(Flags.Flag f) {
        return mfn.getFlag(f);
    }

    public int compareTo(Object o) {
        if (o == null || !(o instanceof MaildirMessage))
            return 0;
        final MaildirMessage m = (MaildirMessage) o;

        return getMaildirFilename().compareTo(m.getMaildirFilename());
    }

    public boolean isFileStateUpdated() {
        return (getFile().getName().equals(mfn.toString()));
    }

    public boolean equals(Object o) {
        if (o == null || !(o instanceof MaildirMessage))
            return false;
        final MaildirMessage m = (MaildirMessage) o;
        if (!isFileStateUpdated())
            return getFile().getName().equals(m.getFile().getName());

        return getMaildirFilename().equals(m.getMaildirFilename());

    }

    public Date getReceivedDate() {
        return new Date(getFile().lastModified());
    }

    public void setReceivedDate(Date receivedDate) {
        getFile().setLastModified(receivedDate.getTime());
    }

    //todo: write testcase
    public InputStream getInputStream() throws MessagingException, IOException {
        parse();
        return super.getInputStream();
    }

    /**
     * Writes text of the message only, but no header.
     */
    public InputStream getRawInputStream() throws MessagingException {
        try {
            InputStream in = new BufferedInputStream(new FileInputStream(getFile()));

            int[] ring = { 0, 0, 0, 0 };
            int position = 0;
            int i = -1;
            // searches for the pattern \r\r, \n\n, \r\n\r\n and returns in when found
            while ((i = in.read()) != -1) {
                ring[position % 4] = i;
                if (position > 0) {
                    // \n\n
                    if (ring[position % 4] == 10
                            && ring[(position - 1) % 4] == 10)
                        return in;
                    // \r\r
                    if (ring[position % 4] == 13
                            && ring[(position - 1) % 4] == 13)
                        return in;
                    // \r\n \r\n
                    if (position > 3) {
                        if (ring[(position - 3) % 4] == 13
                                && ring[(position - 2) % 4] == 10
                                && ring[(position - 1) % 4] == 13
                                && ring[position % 4] == 10)
                            return in;
                    }
                }
                position++;
            }
            // EOF
            return in;
        } catch (IOException e) {
            throw new MessagingException("cannot retrieve message", e);
        }
    }

    public Enumeration getAllHeaderLines() throws MessagingException {
        parse();
        return super.getAllHeaderLines();
    }

    public Enumeration getMatchingHeaders(String[] strings) throws MessagingException {
        parse();
        return super.getMatchingHeaders(strings);
    }

    public Enumeration getMatchingHeaderLines(String as[]) throws MessagingException {
        parse();
        return super.getMatchingHeaderLines(as);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        if (parseStream != null) closeQuietly(parseStream);
    }
}
