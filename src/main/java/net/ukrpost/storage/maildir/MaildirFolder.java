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

import net.ukrpost.storage.javamail.UIDNextAware;
import net.ukrpost.storage.javamail.UIDPlusFolder;
import net.ukrpost.utils.*;

import javax.mail.*;
import javax.mail.event.FolderEvent;
import javax.mail.event.MessageChangedEvent;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * <b>Approach to external deliveries.</b> <p/> External deliveries are only
 * detected in methods that are used to poll maildir:
 * <ul>
 * <li>getMessageCount</li>
 * <li>getNewMessageCount</li>
 * <li>getDeletedMessageCount</li>
 * <li>getUnreadMessageCount</li>
 * </ul>
 * Any other methods expect internal state to be equal to external state. For
 * folder to be aware of externaly delivered messages you should either close
 * and reopen MaildirFolder (slow on large Maildirs) or perform continious
 * polling using methods specified above.
 */
public class MaildirFolder extends Folder implements UIDPlusFolder,
        UIDNextAware {
    private static final Flags supportedFlags = new Flags();

    static {
        supportedFlags.add(Flags.Flag.ANSWERED);
        supportedFlags.add(Flags.Flag.DELETED);
        supportedFlags.add(Flags.Flag.DRAFT);
        supportedFlags.add(Flags.Flag.FLAGGED);
        supportedFlags.add(Flags.Flag.RECENT);
        supportedFlags.add(Flags.Flag.SEEN);
    }

    private static final Message[] EMPTY_MESSAGES = new Message[0];

    private final File rootdir;

    private final File dir;

    private final File curd;

    private final File newd;

    private final File tmpd;

    private LastModified lastModified;

    private String fullFolderName;

    private final boolean isdefault;

    private boolean isopen = false;

    private FolderStats folderStats;

    private ArrayList messages = new ArrayList();

    private TreeMap uniqToMessageMap = new TreeMap();

    private UidsBidiMap uids = null;

    private final MaildirStore store;

    private boolean noQuota;

    public MaildirFolder(String folderName, MaildirStore store) {
        super(store);
        this.store = store;
        rootdir = store.getMaildirRoot();

        fullFolderName = folderName.replace(File.separatorChar, '.');
        fullFolderName = BASE64MailboxEncoder.encode(fullFolderName);

        isdefault = ".".equals(fullFolderName);

        if (fullFolderName.toUpperCase().equals("INBOX")) {
            fullFolderName = ".";
        }

        if (fullFolderName.charAt(0) != '.') {
            fullFolderName = '.' + fullFolderName;
        }
        if (fullFolderName.equals(".")) {
            dir = rootdir;
        } else {
            // operating on "rootdir/." does not work on linux
            dir = new File(rootdir, fullFolderName);
        }
        curd = new File(dir, "cur");
        newd = new File(dir, "new");
        tmpd = new File(dir, "tmp");
    }

    private LastModified newLastModified() {
        String[] curList = getCurDir().list();
        String[] newList = getNewDir().list();
        int curl = curList == null ? -1 : curList.length;
        int newl = newList == null ? -1 : newList.length;
        return new LastModified(curl, newl);
    }

    private boolean isModified() {
        LastModified lm = newLastModified();
        boolean modified = isModified(lm);
        if (modified)
            lastModified = lm;
        return modified;
    }

    /**
     * Returns if folder is newer than specified LastModified object
     */
    protected synchronized boolean isModified(LastModified lm) {
        if (lastModified == null)
            return true;
        return !lastModified.equals(lm);
    }

    private void updatemsgs() throws MessagingException {
        updatemsgs(true, false);
    }

    /**
     * Updates message maps and counters: recentMessages, deletedMessages,
     * unreadMessages based on flags set in MaildirFilename.
     */
    private MaildirMessage addMessage(MaildirFilename mfn) {
        MaildirMessage mm;
        // uniqToMessageMap is not synchronized but is only called from synchronized code
        if (!uniqToMessageMap.containsKey(mfn.getUniq())) {
            mm = new MaildirMessage(this, mfn.getFile(), mfn, -1);
            uniqToMessageMap.put(mfn.getUniq(), mm);
        } else {
            mm = (MaildirMessage) uniqToMessageMap.get(mfn.getUniq());
        }

        if (!mm.isSet(Flags.Flag.SEEN))
            folderStats.unread++;

        if (mm.isSet(Flags.Flag.DELETED))
            folderStats.deleted++;

        if (mm.isSet(Flags.Flag.RECENT))
            folderStats.recent++;

        if (!uids.containsKey(mfn.getUniq()))
            uids.addUid(mfn.getUniq());
        
        return mm;
    }

    private void updatemsgs(boolean doNotify) throws MessagingException {
        updatemsgs(doNotify, false);
    }

    private void updatemsgs(boolean doNotify, boolean forceUpdate)
            throws MessagingException {
    	// updatemsgs is only called by synchronized code
        if (!forceUpdate && (!isModified()))
            return;

        if (!isOpen() || isdefault) {
            return;
        }

            if (!exists()) {
                return;
            }
            folderStats = new FolderStats(0, 0, 0, 0);
            ArrayList oldMessages = null;

            if (doNotify) {
                oldMessages = new ArrayList(uniqToMessageMap.values());

            }

            final MaildirFilename[] newMfns = MaildirUtils.listMfns(newd);
            final MaildirFilename[] curMfns = MaildirUtils.listMfns(curd);
            Set newUniqs = new HashSet();

            if (messages == null) {
                messages = new ArrayList(newMfns.length + curMfns.length);
            }

            for (int i = 0; i < newMfns.length; i++) {
                final MaildirFilename mfn = newMfns[i];

                // according to Maildir spec on folder open
                // all files from "new" should be moved to "cur"
                mfn.renameTo(getCurDir());

                mfn.setFlag(Flags.Flag.RECENT);
                String uniq = addMessage(mfn).getMaildirFilename().getUniq();
                newUniqs.add(uniq);
            }

            for (int i = 0; i < curMfns.length; i++) {
                MaildirFilename mfn = curMfns[i];
                File file = mfn.getFile();
                if (!file.getName().equals(mfn.toString())) {
                    mfn.renameTo(getCurDir());
                }
                String uniq = addMessage(mfn).getMaildirFilename()
                        .getUniq();
                newUniqs.add(uniq);
            }
            Set oldUniqs = new HashSet(uniqToMessageMap.keySet());
            oldUniqs.removeAll(newUniqs);
            Set removedUniqs = oldUniqs;
            for (Iterator iterator = removedUniqs.iterator(); iterator
                    .hasNext();) {
                String uniq = (String) iterator.next();
                uniqToMessageMap.remove(uniq);
            }

            final Collection newMessages = uniqToMessageMap.values();

            // log.info("messages after update: "+newMessages);
            if (doNotify) {
                debug("old messages: " + oldMessages);
                debug("new messages: " + newMessages);

                final Collection removedMessages = collectionsSubtract(
                        oldMessages, newMessages);
                debug("removedMessages: " + removedMessages);

                final Collection addedMessages = collectionsSubtract(
                        newMessages, oldMessages);
                debug("addedMessages: " + addedMessages);

                final Message[] added = (Message[]) addedMessages
                        .toArray(EMPTY_MESSAGES);
                final Message[] removed = (Message[]) removedMessages
                        .toArray(EMPTY_MESSAGES);

                if (removedMessages.size() > 0) {
                    notifyMessageRemovedListeners(true, removed);
                }

                if (addedMessages.size() > 0) {
                    notifyMessageAddedListeners(added);
                }
            }

            messages = new ArrayList(newMessages);
            folderStats.total = messages.size();
            final Iterator it = messages.iterator();

            for (int i = 1; it.hasNext(); i++) {
                final MaildirMessage m = (MaildirMessage) it.next();
                m.setMsgNum(i);
            }
            isModified();
        
    }

    public synchronized int getUnreadMessageCount() throws MessagingException {
        if (!exists())
            throw new FolderNotFoundException(this);
        if (isOpen()) {
            updatemsgs();
            return folderStats.unread;
        }
        if (isModified())
            folderStats = newFolderStats();

        return folderStats.unread;
    }

    private FolderStats newFolderStats() {
        int unreadNew = MaildirUtils.getFlaggedCount(newd, Flags.Flag.SEEN,
                false);
        int unreadCur = MaildirUtils.getFlaggedCount(curd, Flags.Flag.SEEN,
                false);

        final int deletedNew = MaildirUtils.getFlaggedCount(newd,
                Flags.Flag.DELETED, true);
        final int deletedCur = MaildirUtils.getFlaggedCount(curd,
                Flags.Flag.DELETED, true);

        int unread = unreadNew + unreadCur;
        int recent = MaildirUtils.countNonIgnored(newd);
        int deleted = deletedNew + deletedCur;
        int total = recent + MaildirUtils.countNonIgnored(curd);
        return new FolderStats(unread, recent, deleted, total);
    }

    public synchronized int getNewMessageCount() throws MessagingException {
        if (!exists())
            throw new FolderNotFoundException(this);
        if (isOpen()) {
            updatemsgs();
            return folderStats.recent;
        }
        if (isModified())
            folderStats = newFolderStats();

        return folderStats.recent;
    }

    public synchronized int getDeletedMessageCount() throws MessagingException {
        if (!exists())
            throw new FolderNotFoundException(this);
        if (isOpen()) {
            updatemsgs();
            return folderStats.deleted;
        }

        if (isModified())
            folderStats = newFolderStats();

        return folderStats.deleted;
    }

    public synchronized int getMessageCount() throws MessagingException {
        if (!exists())
            throw new FolderNotFoundException(this);

        if (isOpen()) {
            updatemsgs();
            return messages.size();
        }

        if (isModified())
            folderStats = newFolderStats();

        return folderStats.total;
    }

    public synchronized boolean hasNewMessages() throws MessagingException {
        return (getNewMessageCount() > 0);
    }

    private boolean checkMessageSizeBeforeAppend() {
        return Boolean.valueOf(
                getMaildirStore().getSessionProperty(
                        "mail.store.maildir.checkmessagesizebeforeappend"))
                .booleanValue();
    }

    public synchronized void setNoQuotaEnforcement(boolean noQuota) {
        this.noQuota = noQuota;
    }

    private boolean noQuotaEnforcement() {
        return noQuota;
    }

    // handles more than one delivery per second
    // todo: 1. deliver to file with name = mfn.getUniq() then rename to name =
    // mfn.toString()
    private static File newUniqFilename(MaildirFilename mfn, File dir,
            boolean useUniqOnly) throws MessagingException {
        File target = new File(dir, useUniqOnly ? mfn.getUniq() : mfn
                .toString());
        int attempt;
        for (attempt = 0; attempt < 100 && target.exists(); attempt++) {
            mfn.resetDeliveryId();
            target = new File(dir, useUniqOnly ? mfn.getUniq() : mfn.toString());
        }
        if (attempt >= 100) {
            throw new MessagingException(
                    "cannot deliver message after 100 attempts");
        }
        return target;
    }

    private OutputStream getTmpFileOutputStream(File tmpFilename,
            MaildirQuota quota) throws IOException {
        final OutputStream os = new NewlineOutputStream(
                new BufferedOutputStream(new FileOutputStream(tmpFilename),
                        4096));

        if (quota == null || noQuotaEnforcement()
                || quota.getStorageLimit() <= 0)
            return os;

        long sizeLimit = quota.getStorageLimit();
        long mailboxSize = quota.getStorageUsage();

        return new QuotaAwareOutputStream(os, (int) (sizeLimit - mailboxSize));
    }

    private void checkBeforeAppend(Message m, MaildirQuota quota)
            throws MessagingException, QuotaExceededException {
        if (quota == null || !checkMessageSizeBeforeAppend())
            return;

        int messageSize = m.getSize();
        if (messageSize <= 0)
            return;

        if ((quota.getStorageUsage() + messageSize) > quota.getStorageLimit()) {
            throw new QuotaExceededException("message (" + messageSize
                    + "bytes) does not fit into mailbox");
        }
    }

    public Message[] addMessages(Message[] msgs) throws MessagingException {
        return doAddMessages(msgs, true);
    }

    // protected to limit access when folder is closed
    protected Message[] doAddMessages(Message[] msgs, boolean updateUids) throws MessagingException {
        if (!exists())
            throw new FolderNotFoundException(this);

        // FIXME this is always False because of !exists()  throw new FolderNotFoundException(this);
        if (store.isAutoCreateDir() && !isOpen() && !exists()) {
            create(Folder.HOLDS_MESSAGES | Folder.HOLDS_FOLDERS);
        }

        MaildirStore maildirStore = getMaildirStore();
        MaildirQuota quota = null;
        if (maildirStore.getStorageLimit() > 0) {
            quota = maildirStore.getQuota("")[0];
            debug("mailboxSize: " + quota.getStorageUsage() + " sizeLimit: "
                    + quota.getStorageLimit());
        }

        final ArrayList addedMessages = new ArrayList(msgs.length);

        Message[] returnAdded = new Message[msgs.length];

        if (!noQuotaEnforcement() && quota != null
                && quota.getStorageLimit() > 0
                && quota.getStorageUsage() > quota.getStorageLimit())
            throw new MessagingException("quota exceeded",
                    new QuotaExceededException("mailbox is full"));

        try {
            for (int i = 0; i < msgs.length; i++) {
                checkBeforeAppend(msgs[i], quota);
                final MaildirFilename mfn = new MaildirFilename();

                File tmptarget = null;
                OutputStream output = null;

                try {
                    tmptarget = newUniqFilename(mfn, getTmpDir(), true);
                    debug("newUniqFilename: " + tmptarget);
                    output = getTmpFileOutputStream(tmptarget, quota);

                    msgs[i].writeTo(output);
                } catch (Exception e) {
                    tmptarget.delete();
                    Throwable cause = getRootException(e);
                    if (cause instanceof QuotaExceededException)
                        throw ((Exception) cause);
                    else
                        throw e;
                } finally {
                    streamClose(output);
                }

                mfn.setSize(tmptarget.length());

                final boolean deliverToNew = (!isOpen() || msgs[i]
                        .isSet(Flags.Flag.RECENT));
                final File target = (deliverToNew) ? newUniqFilename(mfn,
                        getNewDir(), false) : newUniqFilename(mfn, getCurDir(),
                        false);

                debug("rename '" + tmptarget + "' -> '" + target + '\'');

                if (store.isAutoCreateDir() && !target.getParentFile().exists()) {
                    target.getParentFile().mkdirs();
                }

                final boolean movedFromTmpToNew = tmptarget.renameTo(target);

                if (!movedFromTmpToNew) {
                    tmptarget.delete();
                    target.delete();
                    throw new MessagingException("cant rename " + tmptarget
                            + " to " + target);
                }

                mfn.setFlags(msgs[i].getFlags());

                // todo: write testcase for the following
                if (msgs[i].getReceivedDate() != null)
                    target.setLastModified(msgs[i].getReceivedDate().getTime());
                mfn.setFile(target);
                synchronized (this) {
                if (isOpen()) {
                    // notifications work only for opened folders
                    final MaildirMessage mdm = addMessage(mfn);
                    returnAdded[i] = mdm;
                    mdm.setMsgNum(messages.size() + 1);
                    messages.add(mdm);
                    addedMessages.add(mdm);
                } else if (!isOpen() && updateUids) {
                    // would be faster to do this in a second pass
                    loadUids();
                    if (!uids.containsKey(mfn.getUniq()))
                        uids.addUid(mfn.getUniq());
                    returnAdded[i] = new MaildirMessage(this, target, mfn, -1);
                    saveUids();
                }
                }
            }

        } catch (Exception ex) {
            throw new MessagingException("cant append message", ex);
        } finally {
            if (addedMessages.size() > 0)
                notifyMessageAddedListeners((Message[]) addedMessages
                        .toArray(EMPTY_MESSAGES));
        }

        return returnAdded;
    }
    
	/**
	 * invoked on a closed folder, messages are always put in new as recent and
	 * the rest of the Flags are discarded.<br />
	 * On an open Folder the Flags are preserved and the message is moved to
	 * new/cur depending on its RECENT Flag
	 */

    public void appendMessages(Message[] msgs) throws MessagingException {
        doAddMessages(msgs, false);
    }

    public long[] addUIDMessages(Message[] msgs) throws MessagingException {
        Message[] addedMsgs = doAddMessages(msgs, true);
        long[] addedUids = new long[addedMsgs.length];
        
        // It's okay to catch Exceptions but we shouldn't access while loaduids()
        synchronized (this ) {
        for (int i = 0; i < addedMsgs.length; i++) {
            // It's okay to catch Exception because addedUids[i] is allowed to
            // be -1
            try {
                MaildirMessage mdm = (MaildirMessage) addedMsgs[i];
                String uniq = mdm.getMaildirFilename().getUniq();
                long uid = Long.valueOf(uids.get(uniq).toString()).longValue();
                addedUids[i] = uid;
            } catch (Exception e) {
                debug("Exception in appendUIDMessages:" + e);
                addedUids[i] = -1;
            }
        }
        }
        return addedUids;

    }

    /**
     * Unlike Folder objects, repeated calls to getMessage with the same message
     * number will return the same Message object, as long as no messages in
     * this folder have been expunged.
     */
    public synchronized Message getMessage(int msgnum) throws MessagingException {
        debug("getMessage: " + msgnum);
        return getMessages(new int[] { msgnum })[0];
    }

    public synchronized Message[] getMessages(int[] msgs) throws MessagingException {
        if (!isOpen())
            throw new IllegalStateException("folder closed");
        if (!exists())
            throw new FolderNotFoundException(this);
        // todo: throw IndexOutOfBoundsException - if the start or end message
        // numbers are out of range.

        if (isdefault) {
            throw new MessagingException(
                    "no messages under root folder allowed");
        }

        final ArrayList outmsgs = new ArrayList(msgs.length);

        for (int i = 0; i < msgs.length; i++) {
            if (messages.size() < msgs[i] || (msgs[i] <= 0)) {
                throw new IndexOutOfBoundsException("message " + msgs[i]
                        + " not available");
            }

            final MaildirMessage mdm = (MaildirMessage) messages
                    .get(msgs[i] - 1);
            outmsgs.add(mdm);
        }

        return (Message[]) outmsgs.toArray(EMPTY_MESSAGES);
    }

    public synchronized Message[] getMessages() throws MessagingException {
        if (!isOpen())
            throw new IllegalStateException("folder closed");
        if (!exists())
            throw new FolderNotFoundException(this);
        return messages == null ? EMPTY_MESSAGES : (Message[]) messages
                .toArray(EMPTY_MESSAGES);
    }

    public synchronized boolean isOpen() {
        return isopen;
    }
    
    public synchronized void close(boolean expunge) throws MessagingException {
        if (!isOpen())
            return;
        if (expunge) {
            expunge();
        }

        // update message filenames
        if (getMode() != Folder.READ_ONLY) {
            final int msgsize = messages.size();
            for (int i = 0; i < msgsize; i++) {
                final MaildirMessage mdm = (MaildirMessage) messages.get(i);
                final MaildirFilename mfn = mdm.getMaildirFilename();
                final File file = mfn.getFile();
                if (!file.getName().equals(mfn.toString())
                        || (mdm.isSet(Flags.Flag.RECENT) && file
                                .getParentFile().getName().equals("new"))) {
                	mfn.renameTo(getCurDir());
                }
            }
        }
        isopen = false;

        messages = null;
        if (folderStats != null)
            folderStats = new FolderStats(folderStats.unread, 0,
                    folderStats.deleted, folderStats.total);
        lastModified = newLastModified();
        uniqToMessageMap = new TreeMap();
        if (mode != Folder.READ_ONLY) {
            saveUids();
        }
        // if folder gets closed while addUIDMessages is working uids should still be readable
        // uids = null;
    }

    public synchronized void open(int mode) throws MessagingException {
        if (isopen)
            return;

        if (store.isAutoCreateDir() && !exists()) {
            create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        }

        if (!exists())
            throw new FolderNotFoundException(this, "folder '" + getName()
                    + "' not found");

        this.mode = mode;
        if (isdefault) {
            return;
        }

        // read in uids from .uidvalidity file
        loadUids();
        isopen = true;
        lastModified = new LastModified(-1, -1);
        updatemsgs(false);
        final String[] keys = (String[]) uids.keySet().toArray(new String[0]);
        if (keys != null) {
            for (int i = 0; i < keys.length; i++) {
                final String uniq = keys[i];
                if (!uniqToMessageMap.containsKey(uniq)) {
                    uids.remove(uniq);
                    debug("removed stale uniq from uidvalidity : " + uniq);
                }
            }
        }
        if (getMode() != Folder.READ_ONLY) {
            saveUids();
        }
    }

    private void loadUids() {
        if (!getUIDVFile().exists()) {
            uids = new UidsBidiMap();
            return;
        }

        try {
            uids = new UidsBidiMap(getUIDVFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveUids() {
        if (getDir().canWrite()) {
            OutputStream uidout = null;
            try {
                uidout = new FileOutputStream(getUIDVFile());
                uids.save(uidout);
            } catch (IOException e) {
                // FIXME this should be logged to error
                e.printStackTrace();
            } finally {
                streamClose(uidout);
            }
        }
    }

    protected MaildirStore getMaildirStore() {
        return (MaildirStore) getStore();
    }

    public synchronized boolean renameTo(Folder f) throws MessagingException {
        if (!exists())
            throw new FolderNotFoundException(this);
        if (isOpen())
            throw new IllegalStateException("cannot rename open folder");

        debug("TRACE: '" + getFullName() + "' renameTo('" + f.getFullName()
                + "')");

        if (!(f instanceof MaildirFolder) || (f.getStore() != super.store)) {
            throw new MessagingException("cant rename across stores");
        }

        final boolean result = dir.renameTo(((MaildirFolder) f).getDir());

        if (result) {
            notifyFolderRenamedListeners(f);
        }

        return result;
    }

    public synchronized boolean delete(boolean recurse) throws MessagingException {
        if (!exists())
            throw new FolderNotFoundException(this);
        if (isOpen())
            throw new IllegalStateException("cannot delete open folder");

        if (isdefault || fullFolderName.equals(".")) {
            throw new MessagingException("cant delete root and INBOX folder");
        }

        if (!exists())
            throw new FolderNotFoundException(this);

        boolean result = true;
        final String[] list = rootdir.list();
        if (!recurse) {
            boolean hasSubfolders = false;
            for (int i = 0; i < list.length; i++)
                if (list[i].startsWith(fullFolderName) && !list[i].equals(fullFolderName)) {
                    hasSubfolders = true;
                    break;
                }
            result = hasSubfolders ? false : rmdir(getDir());
        } else {
            for (int i = 0; i < list.length; i++)
                if (list[i].startsWith(fullFolderName)) {
                    File d = new File(rootdir, list[i]);
                    result = result & rmdir(d);
                }
        }
        if (result)
            notifyFolderListeners(FolderEvent.DELETED);

        return result;
    }

    public synchronized Folder getFolder(String name) throws MessagingException {
        String folderfullname;

        name = name.replace(File.separatorChar, '.');

        if (name.charAt(0) == '.') {
            folderfullname = name;
        } else if (name.equals("INBOX")) {
            folderfullname = "INBOX";
        } else {
            if (fullFolderName.endsWith(".")) {
                folderfullname = fullFolderName + name;
            } else {
                folderfullname = fullFolderName + '.' + name;
            }
        }

        return store.getFolder(folderfullname);
    }

    public synchronized boolean create(int type) throws MessagingException {
        debug("create (" + getFullName() + ')');
        if (isdefault && type != Folder.HOLDS_FOLDERS) {
            throw new IllegalArgumentException("Default folder can only hold other folders");
        }
        if ("INBOX".equals(getFullName()) && type != Folder.HOLDS_MESSAGES) {
            throw new IllegalArgumentException("INBOX folder can only messages");
        }

        if (exists()) {
            return false;
        }

        debug("request to create folder: " + dir);
        debug("creating folder: " + dir.getAbsolutePath());
        dir.mkdirs();

        if (!isdefault) {
            curd.mkdir();
            newd.mkdir();
            tmpd.mkdir();
        }
        lastModified = newLastModified();
        folderStats = new FolderStats(0, 0, 0, 0);
        final boolean result = exists();

        if (result) {
            notifyFolderListeners(FolderEvent.CREATED);
        }

        return result;
    }

    public synchronized int getType() {
        // treat the default folder and the INBOX specially.
        if (isdefault)
            return Folder.HOLDS_FOLDERS;

        if ("INBOX".equals(getFullName()))
            return Folder.HOLDS_MESSAGES;

        // otherwise all maildir folders can hold both folders and messages.
        return (Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES); //
    }

    public char getSeparator() {
        return '.';
    }

    public synchronized Folder[] list(String pattern) throws MessagingException {
        if (!exists())
            throw new FolderNotFoundException(this);

        debug("MaildirStore.list('" + pattern + "')");

        // first check to see if we support this search.
        if (pattern == null) {
            pattern = "%";
        }

        final int firstStar = pattern.indexOf('*');
        final int firstPercent = pattern.indexOf('%');

        // check to make sure this is a supported pattern
        if (((firstStar > -1) && (pattern.indexOf('*', firstStar + 1) > -1))
                || ((firstPercent > -1) && (pattern.indexOf('%',
                        firstPercent + 1) > -1))
                || ((firstStar > -1) && (firstPercent > -1))) {
            throw new MessagingException("list pattern not supported");
        }

        final ArrayList folders = new ArrayList(3);

        if (!exists()) {
            return new Folder[0];
        }

        // no subfolders under INBOX
        if (fullFolderName.equals(".") && !isdefault) {
            return new Folder[0];
        }

        final File[] matchingFiles;

        matchingFiles = rootdir.listFiles(new MaildirFileFilter(pattern));

        final String rootPath = rootdir.getAbsolutePath();

        for (int i = 0; i < matchingFiles.length; i++) {
            String fileName = matchingFiles[i].getAbsolutePath();

            if (fileName.startsWith(rootPath)) {
                fileName = fileName.substring(rootPath.length());
            }

            if (fileName.startsWith(File.separator)) {
                fileName = fileName.substring(File.separator.length());
            }

            fileName.replace(File.separatorChar, getSeparator());

            fileName = BASE64MailboxDecoder.decode(fileName);
            folders.add(store.getFolder(fileName));
        }

        // inbox is a special case.
        if (isdefault) {
            boolean includeInbox = true;
            final int wildcardLocation = Math.max(firstStar, firstPercent);
            final String inbox = "INBOX";

            if (wildcardLocation == -1) {
                includeInbox = pattern.equals(inbox);
            } else {
                if ((wildcardLocation > 0)
                        && (!inbox.startsWith(pattern.substring(0,
                                wildcardLocation)))) {
                    includeInbox = false;
                } else {
                    if ((wildcardLocation < (pattern.length() - 1))
                            && (!inbox
                                    .endsWith(pattern.substring(
                                            wildcardLocation + 1, pattern
                                                    .length() - 1)))) {
                        includeInbox = false;
                    }
                }
            }

            if (includeInbox) {
                folders.add(store.getFolder("INBOX"));
            }
        }

        debug("folders.size: " + folders.size());

        return (Folder[]) (folders.toArray(new Folder[0]));
    }

    public synchronized boolean exists() throws MessagingException {
        boolean direxists;

        if (isdefault) {
            // direxists = dir.exists() && dir.isDirectory();
            debug("dir: " + dir);
            direxists = dir.isDirectory();
        } else {
            /*
             * direxists = dir.exists() && dir.isDirectory() && curd.exists() &&
             * curd.isDirectory() && newd.exists() && newd.isDirectory() &&
             * tmpd.exists() && tmpd.isDirectory();
             */
            direxists = curd.isDirectory() && newd.isDirectory()
                    && tmpd.isDirectory();
        }

        debug("exists ?: " + direxists);

        return direxists;
    }

    public synchronized Folder getParent() throws MessagingException {
        if (dir.equals(rootdir)) {
            throw new MessagingException("already at rootdir cant getParent");
        }

        if (!hasParent()) {
            return store.getDefaultFolder();
        }

        final int lastdot = fullFolderName.lastIndexOf(".");
        String parentstr;

        if (lastdot > 0) {
            parentstr = fullFolderName.substring(0, lastdot);

            return store.getFolder(parentstr);
        }

        return null;
    }

    public synchronized String getFullName() {
        String out = "";

        if (isdefault) {
            return "";
        }

        if (fullFolderName.equals(".")) {
            out = "INBOX";
        } else {
            // TODO clarify if fullFolderName should depend on hasParent()
            if (hasParent()) {
                out = fullFolderName;
            } else {
                out = fullFolderName.substring(1);
            }
        }

        out = BASE64MailboxDecoder.decode(out);

        return out;
    }

    /**
     * Returns whether this folder has parent folder. If two folders
     * ".hello.world" and ".hello.world.i.am.here" exist then the second one has
     * parent and the parent is the first one ".hello.world".
     * 
     * @return
     */
    private boolean hasParent() {
        StringTokenizer stk = new StringTokenizer(fullFolderName.substring(0,
                fullFolderName.lastIndexOf('.')), ".");
        StringBuffer sb = new StringBuffer();
        while (stk.hasMoreTokens()) {
            sb.append('.').append(stk.nextToken());
            File tmpparent = new File(rootdir, sb.toString());
            final boolean result = (!tmpparent.equals(rootdir))
                    && tmpparent.isDirectory();

            return result;
        }
        return false;
    }

    public synchronized String getName() {
        String out;

        if (isdefault) {
            out = "";
        }

        if (fullFolderName.equals(".")) {
            out = "INBOX";
        } else {
            if (hasParent()) {
                out = fullFolderName.substring(fullFolderName.lastIndexOf(".") + 1);
            } else {
                out = fullFolderName.substring(1);
            }
        }

        out = BASE64MailboxDecoder.decode(out);

        return out;
    }

    public synchronized Message[] expunge() throws MessagingException {
        if (!isOpen())
            throw new IllegalStateException("folder closed");
        if (!exists())
            throw new FolderNotFoundException(this);

        if (messages == null)
            throw new RuntimeException("internal error: messages == null");

        final List removedMessagesList = new ArrayList();
        boolean forceUpdate = false;
        final int msgsSize = messages.size();
        for (int i = msgsSize - 1; i >= 0; i--) {
            final MaildirMessage mdm = (MaildirMessage) messages.get(i);

            if (mdm.isSet(Flags.Flag.DELETED)) {
                final String uniq = mdm.getMaildirFilename().getUniq();
                uids.remove(uniq);
                uniqToMessageMap.remove(uniq);
                debug("uniq2message: " + uniqToMessageMap.toString());
                messages.remove(mdm);

                File file = mdm.getFile();
                final boolean isDeleted = file.delete();
                debug("removing " + mdm.getFile() + ": " + isDeleted);
                removedMessagesList.add(mdm);
                forceUpdate = true;
            }
        }
        updatemsgs(true, forceUpdate);
        final Message[] removedMessages = (Message[]) removedMessagesList
                .toArray(EMPTY_MESSAGES);
        notifyMessageRemovedListeners(true, removedMessages);

        return removedMessages;
    }

    /**
     * Exposes notifyMessageChangedListeners to package members.
     */
    void localNotifyMessageChangedListeners(int eventType, int eventDetails,
            MaildirMessage changedMessage) throws MessagingException {
        // FIXME: JavaMails Provider Guide says that messageids must stay the
        // same during
        // the whole session and only be updated in expunge() and added in the
        // appendMessages().
        // But when a flag in the maildirmessage changes you broadcast the
        // event,
        // which is ok, _and_ update the messages, this is where ids may get
        // corrupted (shifted up or down) if external delivery (eg from non-java
        // MTA)
        // to this maildir happened.
        // updatemsgs();
        if (eventType == MessageChangedEvent.FLAGS_CHANGED) {
            if ((eventDetails & FlagChangedEvent.ISSET) != 0) {
                if ((eventDetails & FlagChangedEvent.DELETED) != 0)
                    folderStats.deleted++;
                else if ((eventDetails & FlagChangedEvent.RECENT) != 0)
                    folderStats.recent++;
                else if ((eventDetails & FlagChangedEvent.SEEN) != 0)
                    folderStats.unread--;
            } else {
                if ((eventDetails & FlagChangedEvent.DELETED) != 0)
                    folderStats.deleted--;
                else if ((eventDetails & FlagChangedEvent.RECENT) != 0)
                    folderStats.recent--;
                else if ((eventDetails & FlagChangedEvent.SEEN) != 0)
                    folderStats.unread++;
            }
        }
        notifyMessageChangedListeners(eventType, changedMessage);
    }

    public Flags getPermanentFlags() {
        return supportedFlags;
    }

    private File uidVFile = null;

    private File getUIDVFile() {
        if (uidVFile == null)
            uidVFile = new File(getDir(), ".uidvalidity");
        return uidVFile;
    }

    public synchronized long getUIDValidity() throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("folder closed");
        }
        return uids.getUidValidity();
    }

    /**
     * The next unique identifier value is the predicted value that will be
     * assigned to a new message in the mailbox. Unless the unique identifier
     * validity also changes (see below), the next unique identifier value MUST
     * have the following two characteristics. First, the next unique identifier
     * value MUST NOT change unless new messages are added to the mailbox; and
     * second, the next unique identifier value MUST change whenever new
     * messages are added to the mailbox, even if those new messages are
     * subsequently expunged. <p/> Note: The next unique identifier value is
     * intended to provide a means for a client to determine whether any
     * messages have been delivered to the mailbox since the previous time it
     * checked this value. It is not intended to provide any guarantee that any
     * message will have this unique identifier. A client can only assume, at
     * the time that it obtains the next unique identifier value, that messages
     * arriving after that time will have a UID greater than or equal to that
     * value.
     * 
     * @return next unique identifier value.
     */
    public synchronized long getUIDNext() {
        if (!isOpen()) {
            throw new IllegalStateException("folder closed");
        }
        return uids.getLastUid() + 1;
    }

    public synchronized Message getMessageByUID(long uid) throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("folder closed");
        }
        final String uniq = (String) uids.getKey(Long.toString(uid));
        if (uniq == null)
            return null;
        else
            return (Message) uniqToMessageMap.get(uniq);
    }

    public synchronized Message[] getMessagesByUID(long start, long end)
            throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("folder closed");
        }
        if (end == LASTUID)
            end = uids.getLastUid();
        debug("getMessagesByUID " + start + ".." + end);
        if (end < start) {
            // throw new IndexOutOfBoundsException("end cannot be lesser than
            // start");
            return EMPTY_MESSAGES;
        }
        if (end == start) {
            final Message m = getMessageByUID(start);
            return m != null ? new Message[] { m } : EMPTY_MESSAGES;
        }

        final ArrayList messages = new ArrayList();
        for (long i = start; i <= end; i++) {
            final Message m = getMessageByUID(i);
            if (m != null)
                messages.add(m);
        }
        return (Message[]) messages.toArray(EMPTY_MESSAGES);
    }

    public synchronized Message[] getMessagesByUID(long[] uidArray)
            throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("folder closed");
        }
        if (uidArray.length == 1) {
            final Message m = getMessageByUID(uidArray[0]);
            return m != null ? new Message[] { m } : EMPTY_MESSAGES;
        }

        final long[] sortedUidArray = new long[uidArray.length];
        System.arraycopy(uidArray, 0, sortedUidArray, 0, uidArray.length);
        Arrays.sort(sortedUidArray);

        final ArrayList messageList = new ArrayList();
        long prevUid = -1;
        for (int i = 0; i < sortedUidArray.length; i++) {
            final long uid = sortedUidArray[i];
            if (uid == prevUid)
                continue;
            final Message m = getMessageByUID(uid);
            if (m != null)
                messageList.add(m);
            prevUid = uid;
        }
        return (Message[]) messageList.toArray(EMPTY_MESSAGES);
    }

    public synchronized long getUID(Message message) throws MessagingException {
        if (!isOpen()) {
            throw new IllegalStateException("folder closed");
        }
        if (!(message instanceof MaildirMessage))
            throw new NoSuchElementException(
                    "message does not belong to this folder");
        final MaildirMessage mdm = (MaildirMessage) message;
        final String uidStr = (String) uids.get(mdm.getMaildirFilename()
                .getUniq());
        if (uidStr == null)
            throw new NoSuchElementException(
                    "message does not belong to this folder");

        long uid;
        try {
            uid = Long.parseLong(uidStr);
        } catch (NumberFormatException nfex) {
            throw new NoSuchElementException(
                    "message does not belong to this folder: "
                            + nfex.getMessage());
        }
        return uid;
    }

    protected File getDir() {
        return dir;
    }

    protected File getCurDir() {
        return curd;
    }

    protected File getTmpDir() {
        return tmpd;
    }

    protected File getNewDir() {
        return newd;
    }

    private static Collection collectionsSubtract(final Collection a,
            final Collection b) {
        final ArrayList list = new ArrayList(a);
        final Iterator it = b.iterator();

        while (it.hasNext()) {
            list.remove(it.next());
        }

        return list;
    }

    /**
     * Finds only matching valid maildir directories.
     */
    class MaildirFileFilter implements FileFilter {
        final String pattern;

        /**
         * Creates a new MaildirFileFilter to match the given pattern.
         */
        public MaildirFileFilter(String pPattern) {
            if (pPattern == null) {
                pPattern = "%";
            }

            if (fullFolderName.endsWith(".")) {
                pattern = fullFolderName + pPattern;
            } else {
                pattern = fullFolderName + '.' + pPattern;
            }
        }

        /**
         * Tests whether or not the specified abstract pathname should be
         * included in a pathname list.
         */
        public boolean accept(File f) {
            // first, only match if it's a directory that has cur, new, and
            // tmp directories under it.
            if (!(f.isDirectory() && (new File(f, "cur")).isDirectory()
                    && (new File(f, "new")).isDirectory() && (new File(f, "tmp"))
                    .isDirectory())) {
                return false;
            }

            String fileName = f.getName();

            // ...and only match directories which match the given string.
            // this is really annoying. it's a shame that regexp doesn't show up
            // until jdk 1.4
            // to work with non-ascii mailbox names
            fileName = BASE64MailboxDecoder.decode(fileName); //

            boolean noRecurse = false;

            int wildcard = pattern.indexOf('*');

            if (wildcard < 0) {
                wildcard = pattern.indexOf('%');
                noRecurse = true;
            }

            if (wildcard < 0) {
                return fileName.equals(pattern);
            }

            if (wildcard > 0) {
                // test the left side.
                if (!fileName.startsWith(pattern.substring(0, wildcard))) {
                    return false;
                }
            }

            if (wildcard != (pattern.length() - 1)) {
                // test the right side.
                if (!fileName.endsWith(pattern.substring(wildcard + 1))) {
                    return false;
                }
            }

            if (noRecurse) {
                if (fileName.substring(wildcard,
                        fileName.length() - (pattern.length() - wildcard) + 1)
                        .indexOf(getSeparator()) > -1) {
                    return false;
                }
            }

            return true;
        }
    }

    private static void streamClose(OutputStream outs) {
        if (outs != null)
            try {
                outs.close();
            } catch (Exception ex) {
            }
    }

    private static void sleep(long usec) {
        try {
            Thread.sleep(usec);
        } catch (Exception ex) {
        }
    }

    // quick hack for recursive deletion
    private static boolean rmdir(File d) {
        try {
            FileUtils.deleteDirectory(d);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    protected void debug(Object message) {
        getMaildirStore().debug(this, message);
    }

    public synchronized long getStorageUsage() throws MessagingException {
        if (!isOpen()) {
            // slow
            return diskUsage(getNewDir()) + diskUsage(getCurDir());
        }

        if (messages == null || messages.isEmpty())
            return 0;
        updatemsgs(true);
        long usage = 0;
        Object[] messages = this.messages.toArray(); // intentional Object
                                                        // array - faster
                                                        // execution
        for (int i = 0; i < messages.length; i++) {
            usage += ((MaildirMessage) messages[i]).getSize();
        }
        return usage;
    }

    private long diskUsage(File dir) {
        long usage = 0;
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    usage += files[i].length();
                }
            }
        }
        return usage;
    }

    protected LastModified getLastModified() {
        if (lastModified != null)
            return lastModified;
        return newLastModified();
    }

    private static Throwable getRootException(Throwable exception) {
        try {
            Throwable cause = null;
            Method getCause = getMethod(exception, "getCause");
            if (getCause != null)
                cause = (Throwable) getCause.invoke(exception, new Object[0]);

            if (cause != null && cause != exception && !cause.equals(exception))
                return getRootException(cause);

            cause = (exception instanceof MessagingException) 
                ? ((MessagingException) exception).getNextException()
                : exception;
            return cause;
        } catch (SecurityException e) {
            throw new RuntimeException(String.valueOf(e));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(String.valueOf(e));
        } catch (IllegalAccessException e) {
            throw new RuntimeException(String.valueOf(e));
        } catch (InvocationTargetException e) {
            throw new RuntimeException(String.valueOf(e));
        }
    }
    
    private static Method getMethod(Object obj, String name) {
        try {
            return obj.getClass().getMethod(name, new Class[0]);
        } catch (SecurityException e) {
            throw new RuntimeException(String.valueOf(e));
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

}
