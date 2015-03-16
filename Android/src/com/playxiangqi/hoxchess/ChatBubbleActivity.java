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
    private View intputLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ".");
        setContentView(R.layout.activity_chat);

        buttonSend = (Button) findViewById(R.id.buttonSend);
        listView = (ListView) findViewById(R.id.listView1);

        intputLayout = findViewById(R.id.input_layout);
        
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        HoxApp.getApp().registerChatActivity(null);
    }
    
    private void syncWithNewMessages() {
        Log.d(TAG, "Sync with new messages:...");
        
        List<ChatMessage> newMessages = HoxApp.getApp().getNewMessages();
        
        Log.d(TAG, "... # of new messages = " + newMessages.size());
        for (ChatMessage msg : newMessages) {
            chatArrayAdapter.add(msg);
        }
        
        if (HoxApp.getApp().getMyTable().isValid()) {
            intputLayout.setVisibility(View.VISIBLE);
        } else {
            intputLayout.setVisibility(View.GONE);
        }
    }
    
    public void onMessageReceived(ChatMessage chatMsg) {
        chatArrayAdapter.add(chatMsg);
    }
    
    private boolean sendChatMessage() {
        final String msg =  chatText.getText().toString();
        chatText.setText("");
        
        ChatMessage chatMsg = new ChatMessage(false, msg);
        chatArrayAdapter.add(chatMsg);
        
        HoxApp.getApp().handleLocalMessage(chatMsg);
        return true;
    }

}