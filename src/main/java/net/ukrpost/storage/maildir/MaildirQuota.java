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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class MaildirQuota {
    public static final String STORAGE = "STORAGE";
    public static final String MESSAGE = "MESSAGE";
    public static final class Resource {
        public final String name;
        public long usage;
        public long limit;

        public Resource(String name, long usage, long limit) {
            this.name = name;
            this.usage = usage;
            this.limit = limit;
        }
    }

    public final String quotaRoot;
    public final Map resources = Collections.synchronizedMap(new HashMap());

    public MaildirQuota(String s) {
        quotaRoot = s;
    }

    public long getResourceUsage(String name) {
        Resource resource = (Resource) resources.get(name);
        if (resource == null) return 0L;
        return resource.usage;
    }

    public long getResourceLimit(String name) {
        Resource resource = (Resource) resources.get(name);
        if (resource == null) return 0L;
        return resource.limit;
    }

    public void setResourceUsage(String name, long usage) {
        Resource resource;
        if (!resources.containsKey(name)) {
            resource = new Resource(name, usage, 0L);
            resources.put(name, resource);
        } else {
            resource = (Resource) resources.get(name);
            resource.usage = usage;
        }
    }

    public void setResourceLimit(String name, long limit) {
        Resource resource;
        if (!resources.containsKey(name)) {
            resource = new Resource(name, 0L, limit);
            resources.put(name, resource);
        } else {
            resource = (Resource) resources.get(name);
            resource.limit = limit;
        }
    }

    public long getStorageUsage() {
        return getResourceUsage("STORAGE");
    }

    public long getStorageLimit() {
        return getResourceLimit("STORAGE");
    }
}
