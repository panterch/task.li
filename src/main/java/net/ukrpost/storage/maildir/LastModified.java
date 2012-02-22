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

class LastModified implements Cloneable {
    private final long curLm;
    private final long newLm;

    public LastModified(long curLm, long newLm) {
        this.curLm = curLm;
        this.newLm = newLm;
    }

    public LastModified(LastModified lm) {
        curLm = lm.curLm;
        newLm = lm.newLm;
    }

    public String toString() {
        return "cur: " + curLm + " new: " + newLm;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final LastModified that = (LastModified) o;

        if (curLm != that.curLm) return false;
        if (newLm != that.newLm) return false;

        return true;
    }
}
