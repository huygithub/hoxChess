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
import android.content.DialogInterface;
import android.support.design.widget.BottomSheetDialog;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * A sheet that contains actions related to a player.
 */
public class PlayerActionSheet extends BottomSheetDialog {

    public enum Action {
        ACTION_SEND_MESSAGE,
        ACTION_INVITE_PLAYER,
        ACTION_JOIN_TABLE,
    }

    private View sendMessageView_;
    private View invitePlayerView_;
    private View joinTableView_;

    public interface ActionListener {
        void onActionClick(Action action);
    }
    private ActionListener listener_;
    void setActionListener(ActionListener listener) { listener_ = listener; }

    public PlayerActionSheet(final Activity activity, PlayerInfo playerInfo) {
        super(activity);

        final String playerId = playerInfo.pid;
        View sheetView = activity.getLayoutInflater().inflate(R.layout.sheet_dialog_player, null);
        setContentView(sheetView);

        TextView playerInfoView = (TextView) sheetView.findViewById(R.id.sheet_player_info);
        sendMessageView_ = sheetView.findViewById(R.id.sheet_send_private_message);
        invitePlayerView_ = sheetView.findViewById(R.id.sheet_invite_to_play);
        joinTableView_ = sheetView.findViewById(R.id.sheet_join_table_of_player);

        playerInfoView.setText(
                activity.getString(R.string.msg_player_info, playerId, playerInfo.rating));

        sendMessageView_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener_ != null) {
                    listener_.onActionClick(Action.ACTION_SEND_MESSAGE);
                }
            }
        });

        invitePlayerView_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener_ != null) {
                    listener_.onActionClick(Action.ACTION_INVITE_PLAYER);
                }
            }
        });

        joinTableView_.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
                if (listener_ != null) {
                    listener_.onActionClick(Action.ACTION_JOIN_TABLE);
                }
            }
        });
    }

    public void hideAction(Action action) {
        switch (action) {
            case ACTION_SEND_MESSAGE:
                sendMessageView_.setVisibility(View.GONE);
                break;
            case ACTION_INVITE_PLAYER:
                invitePlayerView_.setVisibility(View.GONE);
                break;
            case ACTION_JOIN_TABLE:
                joinTableView_.setVisibility(View.GONE);
                break;
        }
    }

    /**
     * A helper function to send a 1:1 to a player on network.
     */
    public static void sendMessageToPlayer(final Activity activity, final String playerId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        // Set up the input
        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setMessage(activity.getString(R.string.dialog_private_message_title, playerId));
        builder.setPositiveButton(R.string.button_send, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                final String msg = input.getText().toString();
                if (!TextUtils.isEmpty(msg)) {
                    NetworkController.getInstance().handlePrivateMessage(playerId, msg);
                }
            }
        });
        builder.setNegativeButton(R.string.button_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // Do nothing!
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

}