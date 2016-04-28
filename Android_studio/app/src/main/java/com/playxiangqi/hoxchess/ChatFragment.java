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

import android.content.Context;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.util.List;

/**
 * Reference:
 *    http://javapapers.com/android/android-chat-bubble/
 */
public class ChatFragment extends Fragment implements MessageManager.EventListener {

    private static final String TAG = "ChatFragment";

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private Button buttonSend;
    private View inputLayout;

    protected boolean inputEnabled_ = true;

    // ------------------------------------------------
    public interface MessageListener {
        void onLocalMessage(ChatMessage chatMsg);
    }
    private MessageListener messageListener_;

    public void setMessageListener(MessageListener listener) {
        messageListener_ = listener;
    }
    // ------------------------------------------------

    public ChatFragment() {
        Log.d(TAG, "[CONSTRUCTOR]");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // TODO: We hack it to support both types of fragments: Chat and Notification.
        if (context instanceof MainActivity) {
            Log.d(TAG, "onAttach: context = MainActivity. Register self with the activity.");
            MainActivity activity = (MainActivity) context;
            if (activity != null) {
                activity.registerChatFragment(this);
            }
        } else {
            Log.d(TAG, "onAttach: context != MainActivity. Do not register.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView...");
        View view = inflater.inflate(R.layout.activity_chat, container, false);

        // -----
        buttonSend = (Button) view.findViewById(R.id.buttonSend);
        listView = (ListView) view.findViewById(R.id.listView);
        listView.setEmptyView(view.findViewById(R.id.emptyView));

        inputLayout = view.findViewById(R.id.input_layout);

        chatArrayAdapter = new ChatArrayAdapter(getActivity(), R.layout.activity_chat_singlemessage);
        listView.setAdapter(chatArrayAdapter);

        chatText = (EditText) view.findViewById(R.id.chatText);
        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    return sendChatMessage();
                }
                return false;
            }
        });
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                sendChatMessage();
            }
        });

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);

        //to scroll the list view to bottom on data change
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });

        syncWithNewMessages();
        //HoxApp.getApp().registerChatActivity(this);

        inputLayout.setVisibility(inputEnabled_ ? View.VISIBLE : View.GONE);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated...");

        //MainActivity activity = (MainActivity) getActivity();
        //activity.onBoardViewCreated(activity);
    }

    @Override
    public void onResume () {
        super.onResume();
        Log.i(TAG, "onResume...");
        //MainActivity activity = (MainActivity) getActivity();
        //activity.onBoardViewResume(activity);

        //setMessageListener(HoxApp.getApp().getNetworkController());
        //HoxApp.getApp().getNetworkController().addListener(this);
        MessageManager.getInstance().addListener(this);
    }

    @Override
    public void onPause () {
        super.onPause();
        Log.i(TAG, "onPause...");
        //MainActivity activity = (MainActivity) getActivity();
        //activity.onBoardViewResume(activity);

        //HoxApp.getApp().getNetworkController().removeListener(this);
        MessageManager.getInstance().removeListener(this);
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        Log.i(TAG, "onDestroy...");
        //HoxApp.getApp().registerChatActivity(null);
    }

    @Override
    public void onDetach () {
        super.onDetach();
        Log.i(TAG, "onDetach...");
    }


    // ----

    protected void syncWithNewMessages() {
        List<MessageInfo> newMessages = MessageManager.getInstance().getMessages();
        Log.d(TAG, "Sync with new messages: # of new messages = " + newMessages.size());

        for (MessageInfo messageInfo : newMessages) {
            ChatMessage chatMsg = processMessage(messageInfo);
            if (chatMsg != null) {
                addMessage(chatMsg);
            }
        }

        MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
    }

    protected ChatMessage processMessage(MessageInfo messageInfo) {
        switch (messageInfo.type) {
            case MESSAGE_TYPE_CHAT_IN_TABLE: // fall through
                ChatMessage chatMsg = new ChatMessage(true, messageInfo.getFormattedString());
                return chatMsg;
            default:
                return null;
        }
    }

    protected void addMessage(ChatMessage chatMsg) {
        if (chatMsg != null) {
            chatArrayAdapter.add(chatMsg);
        }
    }

    @Override
    public void onMessageReceived(MessageInfo messageInfo) {
        ChatMessage chatMsg = processMessage(messageInfo);
        if (chatMsg != null) {
            addMessage(chatMsg);
        }
    }

    public void clearAll() {
        Log.d(TAG, "clearAll messages...");
        chatArrayAdapter.clear();
    }

    private boolean sendChatMessage() {
        final String msg =  chatText.getText().toString();
        chatText.setText("");

        ChatMessage chatMsg = new ChatMessage(false, msg);
        addMessage(chatMsg);

        //if (messageListener_ != null) {
        //    messageListener_.onLocalMessage(chatMsg);
        //}

        HoxApp.getApp().getNetworkController().onLocalMessage(chatMsg);

        return true;
    }
}
