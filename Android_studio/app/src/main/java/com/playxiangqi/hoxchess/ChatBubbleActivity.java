/**
 * http://javapapers.com/android/android-chat-bubble/
 */
package com.playxiangqi.hoxchess;

import java.util.List;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

public class ChatBubbleActivity extends Activity {
    private static final String TAG = "ChatBubbleActivity";

    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private EditText chatText;
    private Button buttonSend;
    private View inputLayout;

    // ------------------------------------------------
    public interface MessageListener {
        void onLocalMessage(ChatMessage chatMsg);
    }
    private MessageListener messageListener_;

    public void setMessageListener(MessageListener listener) {
        messageListener_ = listener;
    }
    // ------------------------------------------------

    private MessageManager.EventListener messageEventListener_ = new  MessageManager.EventListener() {
        @Override
        public void onMessageReceived(MessageInfo messageInfo) {
            displayMessage(messageInfo);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ".");
        setContentView(R.layout.activity_chat);

        buttonSend = (Button) findViewById(R.id.buttonSend);
        listView = (ListView) findViewById(R.id.listView);
        listView.setEmptyView(findViewById(R.id.emptyView));

        inputLayout = findViewById(R.id.input_layout);
        
        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.activity_chat_singlemessage);
        listView.setAdapter(chatArrayAdapter);

        chatText = (EditText) findViewById(R.id.chatText);
        chatText.setOnKeyListener(new OnKeyListener() {
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
        HoxApp.getApp().registerChatActivity(this);

        inputLayout.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        MessageManager.getInstance().addListener(messageEventListener_);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        MessageManager.getInstance().removeListener(messageEventListener_);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        HoxApp.getApp().registerChatActivity(null);
    }
    
    private void syncWithNewMessages() {
        List<MessageInfo> newMessages = MessageManager.getInstance().getMessages();
        Log.d(TAG, "Sync with new messages: # of new messages = " + newMessages.size());

        for (MessageInfo messageInfo : newMessages) {
            displayMessage(messageInfo);
        }

        MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_INVITE_TO_PLAY);
        MessageManager.getInstance().removeMessages(MessageInfo.MessageType.MESSAGE_TYPE_CHAT_PRIVATE);
    }

    private void displayMessage(MessageInfo messageInfo) {
        switch (messageInfo.type) {
            case MESSAGE_TYPE_INVITE_TO_PLAY: // fall through
            case MESSAGE_TYPE_CHAT_PRIVATE:
                ChatMessage chatMsg = new ChatMessage(true, messageInfo.getFormattedString());
                chatArrayAdapter.add(chatMsg);
                break;
            default:
                //Log.d(TAG, "Ignore other message-type = " + messageInfo.type);
                break;
        }
    }

//    public void onMessageReceived(ChatMessage chatMsg) {
//        chatArrayAdapter.add(chatMsg);
//    }
    
    private boolean sendChatMessage() {
        final String msg =  chatText.getText().toString();
        chatText.setText("");
        
        ChatMessage chatMsg = new ChatMessage(false, msg);
        chatArrayAdapter.add(chatMsg);

        if (messageListener_ != null) {
            messageListener_.onLocalMessage(chatMsg);
        }
        return true;
    }

}