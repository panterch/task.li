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

import javax.mail.Flags;
import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;

public class MaildirUtils {
    private final static MaildirFilename[] EMPTY_MFNS = new MaildirFilename[]{};
    
    private final static class NonIgnored implements FileFilter {
        
        public boolean accept(File file) {
            return (!file.getName().startsWith(".") && file.isFile());
        }
    }
    private final static NonIgnored NON_IGNORED = new NonIgnored();


    public final static File[] listNonIgnored(File dir) {
        return dir.listFiles(NON_IGNORED);
    }

    public final static int countNonIgnored(File dir) {
        File[] files=dir.listFiles(NON_IGNORED);
        if (files==null) {
            return 0;
        } else {
            return files.length;    
        }
    }
    
    public final static MaildirFilename[] listMfns(File dir) {
        final File[] files = listNonIgnored(dir);
        if (files == null)
            return EMPTY_MFNS;

        final ArrayList messages = new ArrayList(files.length);

        for (int i = 0; i < files.length; i++) {
            /*
             * check ".*" files to avoid adding deleted NFS files
             * http://www.qmail.org/man/man5/maildir.html It is a good idea for
             * readers to skip all filenames in new and cur starting with a dot.
             * Other than this, readers should not attempt to parse filenames.
             */

            //TODO: should we extend MaildirFilename from File?
            final MaildirFilename mfn = new MaildirFilename(files[i]);
            if (!files[i].getName().startsWith(mfn.getUniq()))
                mfn.setHostname(files[i].getName());
            messages.add(mfn);
        }

        return (MaildirFilename[]) messages.toArray(EMPTY_MFNS);
    }

    /**
     * Checks whether given flag is set or unset.
     * Example: to check for unseen messages call: getFlaggedCount(dir, Flags.Flag.SEEN, false)
     *
     * @param dir
     * @param flag
     * @param flagState
     * @return
     */
    public final static int getFlaggedCount(File dir, Flags.Flag flag, boolean flagState) {
        final File[] files = listNonIgnored(dir);
        if (files == null)
            return 0;

        int result = 0;
        for (int i = 0; i < files.length; i++) {
            //TODO: should we extend MaildirFilename from File?
            final MaildirFilename mfn = new MaildirFilename(files[i]);

            if (mfn.getFlag(flag) == flagState)
                result++;
        }

        return result;
    }

}
