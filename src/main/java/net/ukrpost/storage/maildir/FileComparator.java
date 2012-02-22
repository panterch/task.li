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

import java.io.File;
import java.util.Comparator;

public final class FileComparator
        implements Comparator {
    public FileComparator() {
    }

    public int compare(Object o1, Object o2) {
        if (!(o1 instanceof File)
                || !(o2 instanceof File)
        )
            return 0;

        final File f1 = (File) o1;
        final File f2 = (File) o2;

        //System.out.println("DEBUG: compare("+f1.getName()+", "+f2.getName()+")");

        return f1.getName().compareTo(f2.getName());
    }

    public boolean equals(Object obj) {
        return false;
    }
}

