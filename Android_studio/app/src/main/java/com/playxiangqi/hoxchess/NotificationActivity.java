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

import java.util.List;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;

public class NotificationActivity extends AppCompatActivity
                    implements ChatFragment.OnChatFragmentListener {

    private static final String TAG = "NotificationActivity";

    private Fragment notificationFragment_;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ".");
        setContentView(R.layout.activity_notifications);

        if (savedInstanceState == null) {
            notificationFragment_ = new NotificationFragment();
            Log.d(TAG, "onCreate: (NEW): Created notification-fragment = " + notificationFragment_);

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, notificationFragment_, "notifications")
                    .commit();
        } else {
            notificationFragment_ = getSupportFragmentManager().findFragmentByTag("notifications");
            Log.d(TAG, "onCreate: (savedInstanceState): Found notification-fragment = " + notificationFragment_);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "(ActionBar) onOptionsItemSelected");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long);
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home: // To handle the BACK button!
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /** Implementation of ChatFragment.OnChatFragmentListener */
    @Override
    public void onChatFragment_CreateView(ChatFragment fragment) {}
    @Override
    public void onChatFragment_DestroyView(ChatFragment fragment) {}

    /**
     * The fragment that handles notifications.
     */
    public static class NotificationFragment extends ChatFragment {
        private static final String TAG = "NotificationFragment";

        public NotificationFragment() {
            Log.d(TAG, "[CONSTRUCTOR]");

            inputEnabled_ = false;
        }

        @Override
        protected ChatMessage processMessage(MessageInfo messageInfo) {
            switch (messageInfo.type) {
                case MESSAGE_TYPE_INVITE_TO_PLAY: // fall through
                case MESSAGE_TYPE_CHAT_PRIVATE:
                    return new ChatMessage(true, getFormattedString(messageInfo));
                default:
                    return null;
            }
        }

        @Override
        protected void syncWithNewMessages() {
            List<MessageInfo> newMessages = MessageManager.getInstance().getMessages();
            Log.d(TAG, "Sync with new messages: # of new messages = " + newMessages.size());

            for (MessageInfo messageInfo : newMessages) {
                ChatMessage chatMsg = processMessage(messageInfo);
                if (chatMsg != null) {
                    addMessage(chatMsg);
                }
            }

            MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_INVITE_TO_PLAY);
            MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_PRIVATE);
        }

        private static String getFormattedString(MessageInfo message) {
            switch (message.type) {
                case MESSAGE_TYPE_INVITE_TO_PLAY: {
                    final String tableIdString = (TextUtils.isEmpty(message.tableId) ? "?" : message.tableId);
                    return "*INVITE: From [" + message.senderPid + " (" + message.senderRating + ")]"
                            + " @ [" + tableIdString + "]";
                }
                case MESSAGE_TYPE_CHAT_PRIVATE: {
                    return "(PRIVATE) " + message.senderPid + ": " + message.content;
                }
                default:
                    return "[]";
            }
        }

    }
}