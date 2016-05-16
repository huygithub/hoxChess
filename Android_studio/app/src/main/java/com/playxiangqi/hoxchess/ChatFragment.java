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
public class ChatFragment extends Fragment {

    private static final String TAG = "ChatFragment";
    private boolean DEBUG_LIFE_CYCLE = true;

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;

    protected boolean inputEnabled_ = true;

    private OnChatFragmentListener listener_;

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnChatFragmentListener {
        void onChatFragment_CreateView(ChatFragment fragment);
        void onChatFragment_DestroyView(ChatFragment fragment);
    }

    public ChatFragment() {
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "[CONSTRUCTOR]");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onAttach");
        if (context instanceof OnChatFragmentListener) {
            listener_ = (OnChatFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnChatFragmentListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onCreateView...");
        View view = inflater.inflate(R.layout.activity_chat, container, false);

        // -----
        Button buttonSend = (Button) view.findViewById(R.id.buttonSend);
        listView = (ListView) view.findViewById(R.id.listView);
        listView.setEmptyView(view.findViewById(R.id.emptyView));

        View inputLayout = view.findViewById(R.id.input_layout);

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

        inputLayout.setVisibility(inputEnabled_ ? View.VISIBLE : View.GONE);

        listener_.onChatFragment_CreateView(this);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onActivityCreated...");
    }

    @Override
    public void onResume () {
        super.onResume();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onResume...");
    }

    @Override
    public void onPause () {
        super.onPause();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onPause...");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDestroyView");
        listener_.onChatFragment_DestroyView(this);
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDestroy...");
    }

    @Override
    public void onDetach () {
        super.onDetach();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDetach...");
    }

    public void addNewMessage(MessageInfo messageInfo) {
        ChatMessage chatMsg = processMessage(messageInfo);
        if (chatMsg != null) {
            addMessage(chatMsg);
        }
    }

    public void addNewMessages(List<MessageInfo> newMessages) {
        for (MessageInfo messageInfo : newMessages) {
            ChatMessage chatMsg = processMessage(messageInfo);
            if (chatMsg != null) {
                addMessage(chatMsg);
            }
        }
    }

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

    protected void addMessage(ChatMessage chatMsg) {
        if (chatMsg != null) {
            chatArrayAdapter.add(chatMsg);
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
