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
package net.ukrpost.utils;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class QuotaAwareOutputStream extends FilterOutputStream {
    private int limit = 0;

    private int usage = 0;

    public QuotaAwareOutputStream(OutputStream os) {
        super(os);
    }

    public QuotaAwareOutputStream(OutputStream os, int quotalimit) {
        super(os);
        limit = quotalimit;
    }

    public void write(int b) throws IOException {
        if (limit != 0 && usage > limit)
            throw new QuotaExceededException("quota limit (" + limit
                    + " bytes) reached");

        usage++;

        super.write(b);
    }

}
