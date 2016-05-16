/**
 *  Copyright 2016 Huy Phan <huyphan@playxiangqi.com>
 *
 *  This file is part of HOXChess.
 *
 *  HOXChess is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  HOXChess is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with HOXChess.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.playxiangqi.hoxchess;

import android.text.TextUtils;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A message
 */
public class MessageInfo {

    public enum MessageType {
        MESSAGE_TYPE_CHAT_IN_TABLE,
        MESSAGE_TYPE_CHAT_PRIVATE,
        MESSAGE_TYPE_INVITE_TO_PLAY
    }

    private final static AtomicInteger messageIdGenerator_ = new AtomicInteger(0);
    private final int messageId_;

    private boolean isRead_ = false;  // Whether it has been read or not?

    public final MessageType type;
    public final String senderPid;

    public String content;  // This can be optional. INVITE is such a type.
    public String senderRating = "1500";  // The default rating

    // Either TO player (e.g., the recipient) or table can be empty.
    public String toPid;
    public String tableId;

    /** Constructor */
    public MessageInfo(MessageType messageType, String fromPlayer) {
        messageId_ = messageIdGenerator_.incrementAndGet();
        type = messageType;
        senderPid = fromPlayer;
    }

    public int getId() { return messageId_; }
    public boolean isRead() { return isRead_; }
    public void markRead() { isRead_ = true; }

    @Override
    public String toString() {
        return String.format("[%s] %s (%s) => [%s]: (%s) @[%s]",
                type.toString(), senderPid, senderRating, toPid, content, tableId);
    }

//    public String getFormattedString() {
//        switch (type) {
//            case MESSAGE_TYPE_CHAT_IN_TABLE: {
//                return senderPid + ": " + content;
//            }
//            case MESSAGE_TYPE_INVITE_TO_PLAY: {
//                final String tableIdString = (TextUtils.isEmpty(tableId) ? "?" : tableId);
//                return "*INVITE: From [" + senderPid + " (" + senderRating + ")]"
//                        + " @ [" + tableIdString + "]";
//            }
//            case MESSAGE_TYPE_CHAT_PRIVATE: {
//                return "(PRIVATE) " + senderPid + ": " + content;
//            }
//            default:
//                return "[]";
//        }
//    }

}
