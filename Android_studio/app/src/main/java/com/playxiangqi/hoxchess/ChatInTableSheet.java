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
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** TODO: A work-in-progress */
public class ChatInTableSheet extends BottomSheetDialog {
    private FloatingActionButton editModeButton;

    public ChatInTableSheet(final Activity activity, final BaseTableController tableController, TableInfo tableInfo) {
        super(activity);

        View sheetView = activity.getLayoutInflater().inflate(R.layout.sheet_dialog_table_chat, null);
        setContentView(sheetView);

        final TextView playerInfoView = (TextView) sheetView.findViewById(R.id.sheet_player_info);
        playerInfoView.setText("Conversation (Table #" + tableInfo.tableId + ")");

        final View inputHeader = sheetView.findViewById(R.id.chat_input_layout_header);
        final View sendButton = sheetView.findViewById(R.id.sheet_comment_button_header);

        final EditText editText = (EditText) sheetView.findViewById(R.id.sheet_edit_text);
        editText.addTextChangedListener(new TextWatcher(){
            public void afterTextChanged(Editable s) {
                int len = editText.getText().toString().length();
                //Log.d("ChatInTableSheet", "# of chars = " + len);
                if (len > 0 && editModeButton.getVisibility() == View.VISIBLE) {
                    editModeButton.setVisibility(View.GONE);
                    sendButton.setVisibility(View.VISIBLE);
                } else if (len == 0 && editModeButton.getVisibility() == View.GONE) {
                    editModeButton.setVisibility(View.VISIBLE);
                    sendButton.setVisibility(View.GONE);
                }
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after){}
            public void onTextChanged(CharSequence s, int start, int before, int count){}
        });

        editModeButton = (FloatingActionButton) sheetView.findViewById(R.id.floating_action_edit_mode);
        editModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("ChatInTableSheet", "Edit mode button clicked!");
                if (playerInfoView.getVisibility() == View.VISIBLE) {
                    playerInfoView.setVisibility(View.GONE);
                    //editModeButton.setVisibility(View.GONE);
                    //editText.setVisibility(View.VISIBLE);
                    inputHeader.setVisibility(View.VISIBLE);
                } else {
                    playerInfoView.setVisibility(View.VISIBLE);
                    //editText.setVisibility(View.GONE);
                    inputHeader.setVisibility(View.GONE);
                    sendButton.setFocusable(false);
                    InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
                }
                //Toast.makeText(activity, "Edit mode button clicked", Toast.LENGTH_SHORT).show();
            }
        });
        //---


        // *******************
        //final BottomSheetBehavior behavior = BottomSheetBehavior.from(sheetView);
        //if (behavior == null) {
        //    Toast.makeText(activity, "Could not find behavior!", Toast.LENGTH_LONG);
        //}
        // *******************

        /*
        final ListView listView = (ListView) sheetView.findViewById(R.id.sheet_chat_listView);
        //listView.setEmptyView(view.findViewById(R.id.emptyView));
        final SheetChatArrayAdapter chatArrayAdapter = new SheetChatArrayAdapter(activity, R.layout.activity_chat_singlemessage);
        listView.setAdapter(chatArrayAdapter);

        for (int i = 1; i < 2; ++i) {
            chatArrayAdapter.add(new ChatMessage(true, "Message #" + i));
        }
        chatArrayAdapter.add(new ChatMessage(false, "This is my message"));
        chatArrayAdapter.add(new ChatMessage(true, "Another friend 's message #3"));

        // Scroll to bottom.
        listView.post(new Runnable() {
            @Override
            public void run() {
                // Select the last row so it will scroll into view...
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });
        */



        List<ChatMessage> chatMessages = new ArrayList<ChatMessage>();
        for (int i = 1; i < 3; ++i) {
            chatMessages.add(new ChatMessage(true, "(RecycleView) Message #" + i));
        }

        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.sheet_chat_recycler_view);

        MoviesAdapter mAdapter = new MoviesAdapter(chatMessages);
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(activity);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(mAdapter);

        //recyclerView.setNestedScrollingEnabled(false);

//        closeTableView_.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                tableController.handleRequestToCloseCurrentTable();
//                dismiss(); // this the dialog.
//            }
//        });

//        resetTableView_.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                //handleRequestToResetTable();
//                //HoxApp.getApp().getNetworkController().handleRequestToInvite(playerId);
//                dismiss(); // this the dialog.
//            }
//        });

        //joinView_.setVisibility(View.GONE);
    }

//    public void hideAction(String action) {
//        if ("close".equals(action)) {
//            closeTableView_.setVisibility(View.GONE);
//        }
//    }
//
//    public void setOnClickListener_Reset(View.OnClickListener listener) {
//        resetTableView_.setOnClickListener(listener);
//    }
//
//    public void setOnClickListener_ReverseBoardView(View.OnClickListener listener) {
//        reverseBoardView_.setOnClickListener(listener);
//    }


    private static class SheetChatArrayAdapter extends ArrayAdapter<ChatMessage> {

        private TextView chatText;
        private List<ChatMessage> chatMessageList = new ArrayList<ChatMessage>();
        private LinearLayout singleMessageContainer;

        @Override
        public void add(ChatMessage object) {
            chatMessageList.add(object);
            super.add(object);
        }

        @Override
        public void clear() {
            chatMessageList.clear();
            super.clear();
        }

        public SheetChatArrayAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public int getCount() {
            return this.chatMessageList.size();
        }

        public ChatMessage getItem(int index) {
            return this.chatMessageList.get(index);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            if (row == null) {
                LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                row = inflater.inflate(R.layout.activity_chat_singlemessage, parent, false);
            }
            singleMessageContainer = (LinearLayout) row.findViewById(R.id.singleMessageContainer);
            ChatMessage chatMessageObj = getItem(position);
            chatText = (TextView) row.findViewById(R.id.singleMessage);
            chatText.setText(chatMessageObj.message);
            chatText.setBackgroundResource(chatMessageObj.left ? R.drawable.bubble_b : R.drawable.bubble_a);
            singleMessageContainer.setGravity(chatMessageObj.left ? Gravity.LEFT : Gravity.RIGHT);
            return row;
        }

        public Bitmap decodeToBitmap(byte[] decodedByte) {
            return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
        }

    }

    public class MoviesAdapter extends RecyclerView.Adapter<MoviesAdapter.MyViewHolder> {

        private List<ChatMessage> messages_;

        public class MyViewHolder extends RecyclerView.ViewHolder {
            //public TextView title, year, genre;
            public LinearLayout singleMessageContainer;
            private TextView chatText;

            public MyViewHolder(View view) {
                super(view);
                //title = (TextView) view.findViewById(R.id.title);
                //genre = (TextView) view.findViewById(R.id.genre);
                //year = (TextView) view.findViewById(R.id.year);
                singleMessageContainer = (LinearLayout) view.findViewById(R.id.singleMessageContainer);
                chatText = (TextView) view.findViewById(R.id.singleMessage);
            }
        }


        public MoviesAdapter(List<ChatMessage> moviesList) {
            this.messages_ = moviesList;
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.activity_chat_singlemessage, parent, false);

            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            ChatMessage msg = messages_.get(position);
            //holder.title.setText(movie.getTitle());
            //holder.genre.setText(movie.getGenre());
            //holder.year.setText(movie.getYear());
            holder.chatText.setText(msg.message);
        }

        @Override
        public int getItemCount() {
            return messages_.size();
        }
    }

}