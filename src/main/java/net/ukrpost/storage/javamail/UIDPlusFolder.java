/***********************************************************************
 * Copyright (c) 2006  Joachim Draeger <jd@joachim-draeger.de>         *
 * All rights reserved.                                                *
 * Licensed under the GNU Lesser General Public License and            * 
 * alternatively under the Apache License, Version 2.0. You may choose *
 * which one fits your needs best and remove the one not suitable as   * 
 * well as this notice.                                                *
 ***********************************************************************
 * This library is free software; you can redistribute it and/or       *
 * modify it under the terms of the GNU Lesser General Public          *
 * License as published by the Free Software Foundation; either        *
 * version 2.1 of the License, or (at your option) any later version.  *
 *                                                                     * 
 * This library is distributed in the hope that it will be useful,     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of      *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU   *
 * Lesser General Public License for more details.                     *
 *                                                                     *
 * You should have received a copy of the GNU Lesser General Public    *
 * License along with this library; if not, write to the Free Software *  
 * Foundation, Inc., 51 Franklin Street,                               *
 * Fifth Floor, Boston, MA  02110-1301  USA                            *
 ***********************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package net.ukrpost.storage.javamail;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;

/**
 * Interim interface to provide access to UID PLUS methods reflecting RFC 2359,
 * until official Javamail API offers this.
 */

public interface UIDPlusFolder extends UIDFolder {
    /**
     * Appends the given messages to the folder and returns corresponding uids.<br>
     * Implementations may require the folder to be open.
     * 
     * @see javax.mail.Folder#appendMessages(javax.mail.Message[])
     * 
     * @param msgs
     *            messages to append
     * @return array of same size and sequenze of msgs containing corresponding
     *         uids or -1, if something went wrong
     * @throws MessagingException
     * @throws IllegalStateException when folder has to be open
     */
    public long[] addUIDMessages(Message[] msgs) throws MessagingException;

    /**
     * Appends the given messages to the folder and returns corresponding
     * instances of the appended messages.<br>
     * Implementations may require the folder to be open.
     * 
     * @see javax.mail.Folder#appendMessages(javax.mail.Message[])
     * 
     * @param msgs
     *            messages to append
     * @return array of same size and sequenze of msgs containing corresponding
     *         added messages or null, if something went wrong
     * @throws MessagingException
     * @throws IllegalStateException when folder has to be open
     */
    public Message[] addMessages(Message[] msgs) throws MessagingException;

}
