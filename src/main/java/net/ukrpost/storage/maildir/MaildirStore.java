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

import javax.mail.*;
import java.io.File;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class MaildirStore extends Store {
    private URLName myurlname = null;
    private File maildirRoot;
    private boolean autoCreateDir;
    private boolean cacheFolders;
    private long quotaSize;
    private long quotaCount;

    public MaildirStore(Session session, URLName urlname) {
        super(session, urlname);
        myurlname = urlname;
        init(new File(urlname.getFile()),
                "true".equals(session.getProperty("mail.store.maildir.autocreatedir")),
                "true".equals(session.getProperty("mail.store.maildir.cachefolders")),
                getQuotaSize(session),
                getQuotaCount(session)
        );

    }

    public MaildirStore(File maildirRoot, long quotaSize) {
        this(maildirRoot, false, false, quotaSize, 0);
    }

    /**
     * @deprecated autocreatedir is deprecated
     */
    public MaildirStore(File maildirRoot, boolean autoCreateDir, boolean cacheFolders, long quotaSize, long quotaCount) {
        super(newSession(autoCreateDir, cacheFolders, quotaSize, quotaCount), urlname(maildirRoot));
        init(maildirRoot, autoCreateDir, cacheFolders, quotaSize, quotaCount);
    }

    void init(File maildirRoot, boolean autoCreateDir, boolean cacheFolders, long quotaSize, long quotaCount) {
        this.maildirRoot = maildirRoot;
        this.autoCreateDir = autoCreateDir;
        this.cacheFolders = cacheFolders;
        this.quotaSize = quotaSize;
        this.quotaCount = quotaCount;

        if (autoCreateDir) {
            maildirRoot.mkdirs();
            debug("creating store: " + maildirRoot.getAbsolutePath() + ": " + maildirRoot.isDirectory());
        }
    }

    public File getMaildirRoot() {
        return maildirRoot;
    }

    //fullname to folder map
    private final Map folders = new HashMap();

    public Folder getFolder(String folderName)
            throws MessagingException {
        MaildirFolder folder = new MaildirFolder(folderName, this);
        // TODO api says they shouldn't be cached. But we have to lock the open ones!
        /*
         * Client has to care about folders state. How should it be able to manage open/close when 
         * optaining multiple instances from store?
         */
        if (!cacheFolders) return folder;
        synchronized (folders) {
            String fullName = folder.getFullName();
            Folder cached = (Folder) folders.get(fullName);
            if (cached != null) return cached;
            folders.put(fullName, cached);
            return cached;
        }
    }

    public Folder getFolder(URLName urlname)
            throws MessagingException {
        return getFolder(urlname.getFile());
    }

    public Folder getDefaultFolder()
            throws MessagingException {
        return getFolder(".");
    }

    protected boolean protocolConnect(String host, int port, String user, String password) {
        return true;
    }

    public void connect() throws MessagingException {
    }

    public void connect(String s, String s1, String s2) throws MessagingException {
    }

    public void connect(String s, int i, String s1, String s2) throws MessagingException {
    }

    public String getSessionProperty(String s) {
        return session.getProperty(s);
    }

    /**
     * String fullName to LastModified
     */
    private final Map mtimes = new HashMap();
    /**
     * String fullName to Long size
     */
    private final Map sizes = new HashMap();
    /**
     * String fullName to Long count
     */
    private final Map counts = new HashMap();

    public MaildirQuota[] getQuota(String root) throws MessagingException {
        Folder[] folders = getDefaultFolder().list();
        long size = 0;
        long count = 0;
        for (int i = 0; i < folders.length; i++) {
            MaildirFolder folder = (MaildirFolder) folders[i];
            if (cacheFolders && !folder.isOpen()) folder.open(Folder.READ_WRITE);
            if (cacheFolders) {
                count += folder.getMessageCount();
                size += folder.getStorageUsage();
            } else {
                synchronized (mtimes) {
                    LastModified lastModified = folder.getLastModified();
                    String fullName = folder.getFullName();
                    LastModified cached = (LastModified) mtimes.get(fullName);
                    if (!lastModified.equals(cached)) {
                        mtimes.put(fullName, lastModified);
                        counts.put(fullName, new Long(folder.getMessageCount()));
                        sizes.put(fullName, new Long(folder.getStorageUsage()));
                    }
                    count += ((Long) counts.get(fullName)).longValue();
                    size += ((Long) sizes.get(fullName)).longValue();
                }
            }
        }
        return new MaildirQuota[]{quota(quotaSize, size, quotaCount, count)};
    }

    //default getURLName in Service doesnot return file which is vital for Maildir
    public URLName getURLName() {
        return myurlname;
    }

    /**
     * @deprecated
     */
    public boolean isAutoCreateDir() {
        return false;
    }

    public long getStorageUsage() throws MessagingException {
        return getQuota("")[0].getStorageUsage();
    }

    public long getStorageLimit() throws MessagingException {
        return quotaSize;
    }

    protected void setStorageUsage(long usage) throws MessagingException {
        getQuota("")[0].setResourceUsage("STORAGE", usage);
    }

    protected void debug(Object context, Object message) {
        PrintStream out = session.getDebugOut();
        if (session.getDebug() && out != null) {
            if (context == null) {
                out.println(message);
            } else {
                out.println(context + " " + message);
            }
        }
    }

    protected void debug(Object message) {
        debug(this, message);
    }

    private static long getQuotaSize(Session session) {
        String size = session.getProperty("mail.store.maildir.quota.size");
        if (size == null || "".equals(size)) return 0;
        try {
            return Long.parseLong(size);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static long getQuotaCount(Session session) {
        String count = session.getProperty("mail.store.maildir.quota.count");
        if (count == null || "".equals(count)) return 0;
        try {
            return Long.parseLong(count);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    private static Session newSession(boolean autoCreateDir, boolean cacheFolders, long quotaSize, long quotaCount) {
        Properties props = new Properties();
        props.setProperty("mail.store.maildir.autocreatedir", autoCreateDir ? "true" : "false");
        props.setProperty("mail.store.maildir.cachefolders", cacheFolders ? "true" : "false");
        props.setProperty("mail.store.maildir.quota.size", Long.toString(quotaSize));
        props.setProperty("mail.store.maildir.quota.count", Long.toString(quotaCount));
        return Session.getInstance(props);
    }

    private static URLName urlname(File maildirRoot) {
        return new URLName("maildir:" + maildirRoot.toString());
    }

    private static MaildirQuota quota(long sizeLimit, long sizeUsage, long countLimit, long countUsage) {
        MaildirQuota q = new MaildirQuota("");
        q.setResourceLimit("STORAGE", sizeLimit);
        q.setResourceLimit("MESSAGE", countLimit);
        q.setResourceUsage("STORAGE", sizeUsage);
        q.setResourceUsage("MESSAGE", countUsage);
        return q;
    }
}
