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

public class ChatFragment extends Fragment implements MessageManager.EventListener {

    private static final String TAG = "ChatFragment";

    // ---
    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private Button buttonSend;
    private View inputLayout;
    // ---

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

        MainActivity activity = (MainActivity) context;
        if (activity != null) {
            activity.registerChatFragment(this);
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

        // Add some sample messages.
        //chatArrayAdapter.add(new ChatMessage(true, "This is a Chat view for one Table only"));
        //chatArrayAdapter.add(new ChatMessage(true, "Messages sent by players in the Table will be displayed here."));

        // -----
        inputLayout.setVisibility(View.VISIBLE);

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

    private void syncWithNewMessages() {
        List<MessageInfo> newMessages = MessageManager.getInstance().getMessages();
        Log.d(TAG, "Sync with new messages: # of new messages = " + newMessages.size());

        for (MessageInfo messageInfo : newMessages) {
            displayMessage(messageInfo);
        }

        MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_IN_TABLE);
    }

    private void displayMessage(MessageInfo messageInfo) {
        switch (messageInfo.type) {
            case MESSAGE_TYPE_CHAT_IN_TABLE: // fall through
                ChatMessage chatMsg = new ChatMessage(true, messageInfo.getFormattedString());
                chatArrayAdapter.add(chatMsg);
                break;
            default:
                //Log.d(TAG, "Ignore other message-type = " + messageInfo.type);
                break;
        }
    }

    @Override
    public void onMessageReceived(MessageInfo messageInfo) {
        displayMessage(messageInfo);
    }

    public void clearAll() {
        Log.d(TAG, "clearAll messages...");
        chatArrayAdapter.clear();
    }

    private boolean sendChatMessage() {
        final String msg =  chatText.getText().toString();
        chatText.setText("");

        ChatMessage chatMsg = new ChatMessage(false, msg);
        chatArrayAdapter.add(chatMsg);

        //if (messageListener_ != null) {
        //    messageListener_.onLocalMessage(chatMsg);
        //}

        HoxApp.getApp().getNetworkController().onLocalMessage(chatMsg);

        return true;
    }
}
