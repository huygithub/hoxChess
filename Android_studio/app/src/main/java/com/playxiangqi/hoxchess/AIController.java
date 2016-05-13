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

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * The controller that controls the AI engine in the AI table.
 */
public class AIController {

    private static final String TAG = "AIController";

    private AIListener boardController_;

    public interface AIListener {
        void onAINewMove(MoveInfo move);
    }

    // The singleton instance.
    private static AIController instance_;

    public static AIController getInstance() {
        if (instance_ == null) {
            instance_ = new AIController();
        }
        return instance_;
    }

    /** The default constructor */
    private AIController() {
        Log.v(TAG, "[CONSTRUCTOR]: ...");
        HoxApp.getApp().getAiEngine().initGame();
    }

    public void setBoardController(AIListener controller) {
        boardController_ = controller;
    }

    /**
     * A message handler to handle UI related tasks.
     */
    private static final int MSG_AI_MOVE_READY = 1;
    private final MessageHandler messageHandler_ = new MessageHandler(this);
    static class MessageHandler extends Handler {

        private final AIController aiController_;
        private Runnable aiRequest_; // Saved so that we could cancel it later.

        public MessageHandler(AIController controller) {
            aiController_ = controller;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_AI_MOVE_READY:
                    aiController_.onAIMoveMade((String) msg.obj);
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

    public void onHumanMove(Position fromPos, Position toPos) {
        AIEngine aiEngine = HoxApp.getApp().getAiEngine();
        aiEngine.onHumanMove(fromPos.row, fromPos.column, toPos.row, toPos.column);
        final long delayMillis = 2000; // Add delay so we have time to observe the move.
        Log.d(TAG, "On human move: Will ask AI to generate a move after some delay:" + delayMillis);
        messageHandler_.postAIRequest(aiEngine, delayMillis);
    }

    public void resetGame() {
        Log.d(TAG, "Reset game...");
        messageHandler_.cancelPendingAIRequest();
        HoxApp.getApp().getAiEngine().initGame();;
    }

    private void onAIMoveMade(String aiMove) {
        Log.d(TAG, "AI returned this move [" + aiMove + "].");

        Position fromPos = new Position(aiMove.charAt(0) - '0', aiMove.charAt(1) - '0');
        Position toPos = new Position(aiMove.charAt(2) - '0', aiMove.charAt(3) - '0');
        MoveInfo move = new MoveInfo(fromPos, toPos);

        move.gameStatus = HoxApp.getApp().getReferee().validateMove(
                fromPos.row, fromPos.column,
                toPos.row, toPos.column);

        if (move.gameStatus == Referee.hoxGAME_STATUS_UNKNOWN) { // Move is not valid?
            Log.e(TAG, " This move = " + move + " is NOT valid. Do nothing.");
            return;
        }

        if (boardController_ != null) {
            boardController_.onAINewMove(move);
        }
    }

}
