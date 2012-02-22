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

import javax.mail.internet.SharedInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;

public class SharedFileInputStream extends BufferedInputStream implements
        SharedInputStream {
    class SharedFile {

        private int cnt;

        private RandomAccessFile in;

        public RandomAccessFile open() {
            cnt++;
            return in;
        }

        public synchronized void close() throws IOException {
            if (--cnt <= 0)
                in.close();
        }

        protected void finalize() throws Throwable {
            super.finalize();
            in.close();
        }

        SharedFile(String s) throws IOException {
            in = new RandomAccessFile(s, "r");
        }
    }

    private static int defaultBufferSize = 2048;

    protected byte buf[];

    protected int count;

    protected int pos;

    protected int markpos;

    protected int marklimit;

    protected RandomAccessFile in;

    protected int bufsize;

    protected long bufpos;

    protected long start;

    protected long datalen;

    private SharedFile sf;

    private void ensureOpen() throws IOException {
        if (in == null)
            throw new IOException("Stream closed");
        else
            return;
    }

    public SharedFileInputStream(String s) throws IOException {
        this(s, defaultBufferSize);
    }

    public SharedFileInputStream(String s, int i) throws IOException {
        super(null);
        markpos = -1;
        if (i <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        } else {
            sf = new SharedFile(s);
            in = sf.open();
            start = 0L;
            datalen = in.length();
            bufsize = i;
            buf = new byte[i];
            return;
        }
    }

    private SharedFileInputStream(SharedFile sharedfile, long l, long l1, int i) {
        super(null);
        markpos = -1;
        sf = sharedfile;
        in = sharedfile.open();
        start = l;
        bufpos = l;
        datalen = l1;
        bufsize = i;
        buf = new byte[i];
    }

    private void fill() throws IOException {
        if (markpos < 0) {
            pos = 0;
            bufpos += count;
        } else if (pos >= buf.length)
            if (markpos > 0) {
                int i = pos - markpos;
                System.arraycopy(buf, markpos, buf, 0, i);
                pos = i;
                bufpos += markpos;
                markpos = 0;
            } else if (buf.length >= marklimit) {
                markpos = -1;
                pos = 0;
                bufpos += count;
            } else {
                int j = pos * 2;
                if (j > marklimit)
                    j = marklimit;
                byte abyte0[] = new byte[j];
                System.arraycopy(buf, 0, abyte0, 0, pos);
                buf = abyte0;
            }
        count = pos;
        in.seek(bufpos + (long) pos);
        int k = buf.length - pos;
        if ((bufpos - start) + (long) pos + (long) k > datalen)
            k = (int) (datalen - ((bufpos - start) + (long) pos));
        int l = in.read(buf, pos, k);
        if (l > 0)
            count = l + pos;
    }

    public synchronized int read() throws IOException {
        ensureOpen();
        if (pos >= count) {
            fill();
            if (pos >= count)
                return -1;
        }
        return buf[pos++] & 0xff;
    }

    private int read1(byte abyte0[], int i, int j) throws IOException {
        int k = count - pos;
        if (k <= 0) {
            fill();
            k = count - pos;
            if (k <= 0)
                return -1;
        }
        int l = k >= j ? j : k;
        System.arraycopy(buf, pos, abyte0, i, l);
        pos += l;
        return l;
    }

    public synchronized int read(byte abyte0[], int i, int j)
            throws IOException {
        ensureOpen();
        if ((i | j | i + j | abyte0.length - (i + j)) < 0)
            throw new IndexOutOfBoundsException();
        if (j == 0)
            return 0;
        int k = read1(abyte0, i, j);
        if (k <= 0)
            return k;
        int l;
        for (; k < j; k += l) {
            l = read1(abyte0, i + k, j - k);
            if (l <= 0)
                break;
        }

        return k;
    }

    public synchronized long skip(long l) throws IOException {
        ensureOpen();
        if (l <= 0L)
            return 0L;
        long l1 = count - pos;
        if (l1 <= 0L) {
            fill();
            l1 = count - pos;
            if (l1 <= 0L)
                return 0L;
        }
        long l2 = l1 >= l ? l : l1;
        pos += l2;
        return l2;
    }

    public synchronized int available() throws IOException {
        ensureOpen();
        return (count - pos) + in_available();
    }

    private int in_available() throws IOException {
        return (int) ((start + datalen) - (bufpos + (long) count));
    }

    public synchronized void mark(int i) {
        marklimit = i;
        markpos = pos;
    }

    public synchronized void reset() throws IOException {
        ensureOpen();
        if (markpos < 0) {
            throw new IOException("Resetting to invalid mark");
        } else {
            pos = markpos;
            return;
        }
    }

    public boolean markSupported() {
        return true;
    }

    public void close() throws IOException {
        if (in == null)
            return;
        try {
            sf.close();
        } finally {
            sf = null;
            in = null;
            buf = null;
        }
    }

    public long getPosition() {
        if (in == null)
            throw new RuntimeException("Stream closed");
        else
            return (bufpos + (long) pos) - start;
    }

    public InputStream newStream(long l, long l1) {
        if (in == null)
            throw new RuntimeException("Stream closed");
        if (l < 0L)
            throw new IllegalArgumentException("start < 0");
        if (l1 == -1L)
            l1 = datalen;
        return new SharedFileInputStream(sf, start + (long) (int) l,
                (int) (l1 - l), bufsize);
    }

    protected void finalize() throws Throwable {
        super.finalize();
        close();
    }

}
