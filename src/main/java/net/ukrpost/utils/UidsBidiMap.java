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

import java.io.*;
import java.util.*;

public class UidsBidiMap implements Map {
    private long lastUid = 0;
    private long uidValidity = System.currentTimeMillis();


    public UidsBidiMap() {
    }

    public UidsBidiMap(File uidsFile) throws IOException {

        InputStream in = null;
        try {
            in = new FileInputStream(uidsFile);
            load(in);
        } finally {
            if (in != null)
                try {
                    in.close();
                } catch (Exception ex) {
                }
        }
    }

    public UidsBidiMap(Map map) {
        putAll(map);
    }

    private final Map key2value = new HashMap();
    private final Map value2key = new HashMap();

    public synchronized int size() {
        return key2value.size();
    }

    public synchronized void clear() {
        key2value.clear();
        value2key.clear();
    }

    public synchronized boolean isEmpty() {
        return key2value.isEmpty();
    }

    public synchronized boolean containsKey(Object key) {
        return key2value.containsKey(key);
    }

    public synchronized boolean containsValue(Object value) {
        return key2value.containsValue(value);
    }

    public synchronized Collection values() {
        return key2value.values();
    }

    public synchronized void putAll(Map map) {
        final Iterator keys = map.keySet().iterator();
        while (keys.hasNext()) {
            final Object key = keys.next();
            final Object value = map.get(key);
            key2value.put(key, value);
            value2key.put(value, key);
        }
    }

    public synchronized Set entrySet() {
        return key2value.entrySet();
    }

    public synchronized Set keySet() {
        return key2value.keySet();
    }

    public synchronized Object get(Object key) {
        return key2value.get(key);
    }

    public synchronized Object getKey(Object value) {
        return value2key.get(value);
    }

    public synchronized Object remove(Object key) {
        final Object value = key2value.get(key);
        value2key.remove(value);
        return key2value.remove(key);
    }

    public synchronized Object put(Object key, Object value) {
        value2key.put(value, key);
        return key2value.put(key, value);
    }

    public synchronized void load(InputStream in) throws IOException {
        final Properties p = new Properties();
        p.load(in);
        String s = p.getProperty("lastuid");
        if (s != null)
            setLastUid(Long.parseLong(s));
        s = p.getProperty("uidvalidity");

        if (s != null)
            setUidValidity(Long.parseLong(s));
        p.remove("lastuid");
        p.remove("uidvalidity");
        putAll(p);
    }

    public synchronized void save(OutputStream out) throws IOException {
        final Properties p = new Properties();
        p.putAll(key2value);
        p.setProperty("lastuid", Long.toString(getLastUid()));
        p.setProperty("uidvalidity", Long.toString(getUidValidity()));
        p.store(out, null);
    }

    public long getLastUid() {
        return lastUid;
    }

    public synchronized void setLastUid(long lastUid) {
        this.lastUid = lastUid;
    }

    public long getUidValidity() {
        return uidValidity;
    }

    public synchronized void setUidValidity(long uidValidity) {
        this.uidValidity = uidValidity;
    }

    public void addUid(String key) {
        put(key, Long.toString(++lastUid));
    }

    public String toString() {
        return "last: " + getLastUid() + " valid: " + getUidValidity() + ' ' + key2value.toString();
    }
}
