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

import com.playxiangqi.hoxchess.Enums.ColorEnum;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.PopupMenu;

/**
 * The controller that manages a local table.
 */
public class LocalTableController extends BaseTableController {

    private static final String TAG = "LocalTableController";

    // My color in the local (ie. to practice with AI).
    // NOTE: Currently, we cannot change my role in this type of table.
    private final ColorEnum myColorInLocalTable_ = ColorEnum.COLOR_RED;

    public LocalTableController() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
    }

    /**
     * A message handler to handle UI related tasks.
     */
    private static final int MSG_AI_MOVE_READY = 1;
    private final MessageHandler messageHandler_ = new MessageHandler(this);
    static class MessageHandler extends Handler {

        private final LocalTableController tableController_;
        private Runnable aiRequest_; // Saved so that we could cancel it later.

        public MessageHandler(LocalTableController controller) {
            tableController_ = controller;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AI_MOVE_READY:
                    tableController_.onAIMoveMade((String) msg.obj);
                    break;

                default:
                    break;
            }
        }

        public void cancelPendingAIRequest() {
            if (aiRequest_ != null) {
                Log.d(TAG, "(MessageHandler) Cancel the pending AI request...");
                removeCallbacks(aiRequest_);
                aiRequest_ = null;
            }
        }

        public void postAIRequest(final AIEngine aiEngine, long delayMillis) {
            aiRequest_ = new Runnable() {
                public void run() {
                    aiRequest_ = null;
                    final String aiMove = aiEngine.generateMove();
                    Log.d(TAG, "... AI returned this move [" + aiMove + "].");
                    sendMessage(obtainMessage(MSG_AI_MOVE_READY, aiMove) );
                }
            };
            postDelayed(aiRequest_, delayMillis);
        }
    }

    @Override
    public boolean handleBackPressed() {
        return false;
    }

    @Override
    public boolean onPrepareOptionsMenu(Context context, Menu menu) {
        menu.findItem(R.id.action_new_table).setVisible(false);
        menu.findItem(R.id.action_close_table).setVisible(false);
        return true; // display the menu
    }

    @Override
    public void onClick_resetTable(final Context context, View view) {
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(R.menu.table_actions, popup.getMenu());

        popup.getMenu().removeItem(R.id.action_offer_draw);
        popup.getMenu().removeItem(R.id.action_offer_resign);
        popup.getMenu().removeItem(R.id.action_close_table);

        if (popup.getMenu().size() == 0) {
            Log.i(TAG, "(on 'Reset' button click) No need to show popup menu!");
            return;
        }

        super.setupListenerForResetButton(context, popup);
        popup.show();
    }

    @Override
    public boolean isMyTurn() {
        Referee referee = HoxApp.getApp().getReferee();
        return (myColorInLocalTable_ == referee.getNextColor());
    }

    @Override
    public void onLocalMove(Position fromPos, Position toPos) {
        Referee referee = HoxApp.getApp().getReferee();
        Log.i(TAG, "On local move: referee 's moveCount = " + referee.getMoveCount());

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        AIEngine aiEngine = HoxApp.getApp().getAiEngine();

        timeTracker.nextColor();

        if (referee.getMoveCount() == 2) {
            timeTracker.start();
            MainActivity mainActivity = mainActivity_.get();
            if (mainActivity != null) {
                mainActivity.onGameStatusChanged();
            }
        }

        if (referee.getMoveCount() > 1) { // The game has started?
            playerTracker.syncUI();
        }

        aiEngine.onHumanMove(fromPos.row, fromPos.column, toPos.row, toPos.column);
        if (!referee.isGameInProgress()) {
            onGameEnded(); // The game has ended. Do nothing
        } else {
            final long delayMillis = 2000; // Add delay so we have time to observe the move.
            Log.d(TAG, "Will ask AI to generate a move after some delay:" + delayMillis);
            messageHandler_.postAIRequest(aiEngine, delayMillis);
        }
    }

    @Override
    protected void handleRequestToResetTable() {
        Log.i(TAG, "Handle request to 'Reset Table'...");

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        AIEngine aiEngine = HoxApp.getApp().getAiEngine();

        messageHandler_.cancelPendingAIRequest();
        playerTracker.syncUI();
        aiEngine.initGame();
        timeTracker.stop();
        timeTracker.reset();
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.openNewPracticeTable();
            Snackbar.make(mainActivity.findViewById(R.id.board_view), R.string.action_reset,
                    Snackbar.LENGTH_SHORT)
                    .show();
        }
    }

    private void onGameEnded() {
        Log.d(TAG, "On game-ended...");
        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.stop();
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.onGameStatusChanged();
        }
    }

    private void onAIMoveMade(String aiMove) {
        Log.d(TAG, "AI returned this move [" + aiMove + "].");

        Referee referee = HoxApp.getApp().getReferee();
        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();

        Position fromPos = new Position(aiMove.charAt(0) - '0', aiMove.charAt(1) - '0');
        Position toPos = new Position(aiMove.charAt(2) - '0', aiMove.charAt(3) - '0');

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.updateBoardWithNewAIMove(fromPos, toPos);
        }

        timeTracker.nextColor();

        if (referee.getMoveCount() == 2) {
            timeTracker.start();
            if (mainActivity != null) {
                mainActivity.onGameStatusChanged();
            }
        }

        if (!referee.isGameInProgress()) {
            Log.i(TAG, "... after AI move => the game has ended.");
            onGameEnded();
        }
    }

}
