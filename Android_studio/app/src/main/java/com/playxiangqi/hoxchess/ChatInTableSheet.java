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

import android.app.Activity;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * A bottom sheet based dialog to display comments/chat in the current network table.
 *
 * */
public class ChatInTableSheet extends BottomSheetDialog {

    private static final String TAG = "ChatInTableSheet";

    private final TableInfo tableInfo_;

    private RecyclerView recyclerView_;
    private ChatInTableAdapter adapter_;

    private EditText editText_;

    /**
     * Constructor
     * */
    public ChatInTableSheet(final Activity activity, List<MessageInfo> newMessages, TableInfo tableInfo) {
        super(activity);
        Log.d(TAG, "[CONSTRUCTOR]");

        tableInfo_ = tableInfo;

        View sheetView = activity.getLayoutInflater().inflate(R.layout.sheet_dialog_table_chat, null);
        setContentView(sheetView);

        final TextView playerInfoView = (TextView) sheetView.findViewById(R.id.sheet_player_info);
        playerInfoView.setText(
                activity.getString(R.string.chat_table_title, tableInfo_.tableId));

        editText_ = (EditText) sheetView.findViewById(R.id.sheet_edit_text);
        editText_.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    return sendMyInput();
                }
                return false;
            }
        });

        final View sendButton = sheetView.findViewById(R.id.sheet_comment_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMyInput();
            }
        });

        // Process the initial list of messages.
        List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
        Log.d(TAG, "Initial list count: " + newMessages.size());
        for (MessageInfo messageInfo : newMessages) {
            ChatMessage chatMsg = processMessage(messageInfo);
            if (chatMsg != null) {
                chatMessages.add(0, chatMsg);
            }
        }

        recyclerView_ = (RecyclerView) findViewById(R.id.sheet_chat_recycler_view);

        adapter_ = new ChatInTableAdapter(chatMessages);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(activity);
        recyclerView_.setLayoutManager(mLayoutManager);
        recyclerView_.setItemAnimator(new DefaultItemAnimator());
        recyclerView_.setAdapter(adapter_);
    }

    public void addNewMessage(MessageInfo messageInfo) {
        ChatMessage chatMsg = processMessage(messageInfo);
        if (chatMsg != null) {
            adapter_.addMessage(chatMsg);
            recyclerView_.scrollToPosition(0);
        }
    }

    private ChatMessage processMessage(MessageInfo messageInfo) {
        switch (messageInfo.type) {
            case MESSAGE_TYPE_CHAT_IN_TABLE:
                if (HoxApp.getApp().getMyPid().equals(messageInfo.senderPid)) { // my own message?
                    return new ChatMessage(false, messageInfo.content);
                } else {
                    return new ChatMessage(true,
                            messageInfo.senderPid + ": " + messageInfo.content);
                }
            default:
                return null;
        }
    }

    private boolean sendMyInput() {
        final String userText = editText_.getText().toString();
        if (TextUtils.isEmpty(userText)) {
            return false;
        }
        editText_.setText("");

        Log.d(TAG, "Send my input: [" + userText + "]");
        ChatMessage chatMsg = new ChatMessage(false, userText);
        adapter_.addMessage(chatMsg);
        recyclerView_.scrollToPosition(0);

        MessageManager.getInstance().addMyMessageInTable(userText, tableInfo_.tableId);
        HoxApp.getApp().getNetworkController().onLocalMessage(chatMsg);
        return true;
    }

    /**
     * The adapter for this table sheet.
     */
    private static class ChatInTableAdapter
            extends RecyclerView.Adapter<ChatInTableAdapter.MyViewHolder> {

        private List<ChatMessage> messages_;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            public LinearLayout singleMessageContainer;
            private TextView chatText;

            public MyViewHolder(View view) {
                super(view);
                singleMessageContainer = (LinearLayout) view.findViewById(R.id.singleMessageContainer);
                chatText = (TextView) view.findViewById(R.id.singleMessage);
            }
        }

        public ChatInTableAdapter(List<ChatMessage> messageList) {
            messages_ = messageList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_chat_singlemessage, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            ChatMessage msg = messages_.get(position);;
            holder.chatText.setText(msg.message);
            holder.chatText.setBackgroundResource(msg.left ? R.drawable.bubble_b : R.drawable.bubble_a);
            holder.singleMessageContainer.setGravity(msg.left ? Gravity.LEFT : Gravity.RIGHT);
        }

        @Override
        public int getItemCount() {
            return messages_.size();
        }

        // ******************************************************************************
        //
        //               Other public APIs
        //
        // *******************************************************************************

        public void addMessage(ChatMessage message) {
            messages_.add(0, message);
            this.notifyItemInserted(0);
        }
    }

}