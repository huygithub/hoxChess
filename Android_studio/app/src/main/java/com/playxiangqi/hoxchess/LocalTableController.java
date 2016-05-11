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

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.Menu;
import android.view.View;

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
    public void setupNewTable() {
        Log.d(TAG, "setupNewTable:...");
        HoxApp.getApp().getAiEngine().initGame();

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        final TimeInfo initialTime = new TimeInfo(Enums.DEFAULT_INITIAL_GAME_TIMES);
        timeTracker.setInitialColor(ColorEnum.COLOR_RED);
        timeTracker.setInitialTime(initialTime);
        timeTracker.setBlackTime(initialTime);
        timeTracker.setRedTime(initialTime);

        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.setTableType(Enums.TableType.TABLE_TYPE_LOCAL); // A new practice table.
        playerTracker.setRedInfo(HoxApp.getApp().getString(R.string.you_label), "1501");
        playerTracker.setBlackInfo(HoxApp.getApp().getString(R.string.ai_label), "1502");
    }

    @Override
    public void onNetworkLoginSuccess() {
        Log.d(TAG, "onNetworkLoginSuccess:...");

        BaseTableController.setCurrentController(Enums.TableType.TABLE_TYPE_EMPTY);
        BaseTableController.getCurrentController().onNetworkLoginSuccess();
    }

    @Override
    public void setTableTitle() {
        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.setAndShowTitle("AI");
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Context context, Menu menu) {
        menu.findItem(R.id.action_new_table).setVisible(false);
        menu.findItem(R.id.action_close_table).setVisible(false);
        return true; // display the menu
    }

    @Override
    public void handleTableMenuOnClick(Activity activity) {
        //if (mainActivity_ == null || context != mainActivity_.get()) {
        //    throw new RuntimeException("The context must be the Main Activity");
        //}

        //MainActivity mainActivity = mainActivity_.get();
        final TableActionSheet actionSheet = new TableActionSheet(activity);
        actionSheet.setHeaderText(activity.getString(R.string.title_table_ai));
        super.setupListenersInTableActionSheet(actionSheet);

        actionSheet.hideAction(TableActionSheet.Action.ACTION_CLOSE_TABLE);
        actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_DRAW);
        actionSheet.hideAction(TableActionSheet.Action.ACTION_OFFER_RESIGN);
        actionSheet.hideAction(TableActionSheet.Action.ACTION_NEW_TABLE);

        actionSheet.show();
    }

    @Override
    public void handleRequestToOpenNewTable() {
        Log.d(TAG, "Request to open a new table...");

        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.setTableType(Enums.TableType.TABLE_TYPE_LOCAL); // A new practice table.

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.stop();

        setupNewTable();

        timeTracker.syncUI();
        playerTracker.syncUI();

        MainActivity mainActivity = mainActivity_.get();
        if (mainActivity != null) {
            mainActivity.setTableController(this);
            mainActivity.openNewPracticeTable();
            mainActivity.invalidateOptionsMenu(); // Recreate the options menu
        }
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
//        if (mainActivity != null) {
//            mainActivity.openNewPracticeTable();
//            Snackbar.make(mainActivity.findViewById(R.id.board_view), R.string.action_reset,
//                    Snackbar.LENGTH_SHORT)
//                    .show();
//        }
        if (boardController_ != null) {
            boardController_.openNewPracticeTable();
            //Snackbar.make(mainActivity.findViewById(R.id.board_view), R.string.action_reset,
            //        Snackbar.LENGTH_SHORT)
            //        .show();
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

        MoveInfo move = new MoveInfo(fromPos, toPos);

        move.gameStatus = referee.validateMove(
                fromPos.row, fromPos.column,
                toPos.row, toPos.column);

        if (move.gameStatus == Referee.hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
            Log.e(TAG, " This move = " + move + " is NOT valid. Do nothing.");
            return;
        }

//        MainActivity mainActivity = mainActivity_.get();
//        if (mainActivity != null) {
//            mainActivity.updateBoardWithNewAIMove(move);
//        }
        if (boardController_ != null) {
            boardController_.updateBoardWithNewMove(move);
        }

        timeTracker.nextColor();

        if (referee.getMoveCount() == 2) {
            timeTracker.start();
        }

        if (!referee.isGameInProgress()) {
            Log.i(TAG, "... after AI move => the game has ended.");
            onGameEnded();
        }
    }

}
