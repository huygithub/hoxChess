/**
 *  Copyright 2015 Huy Phan <huyphan@playxiangqi.com>
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
import com.playxiangqi.hoxchess.Enums.TableType;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A table player tracker
 */
public class TablePlayerTracker {
    private static final String TAG = "TablePlayerTracker";

    private enum SeatMode {
        SEAT_MODE_NONE,
        SEAT_MODE_PLAY,
        SEAT_MODE_LEAVE
    }

    private boolean hasUI_ = false;
    private TextView blackLabel_;
    private Button blackButton_;
    private TextView redLabel_;
    private Button redButton_;

    private TableType tableType_;
    private ColorEnum myColor_ = ColorEnum.COLOR_RED;

    private PlayerInfo blackPlayer_ = new PlayerInfo();
    private PlayerInfo redPlayer_ = new PlayerInfo();

    private SeatMode blackSeatMode_ = SeatMode.SEAT_MODE_NONE;
    private SeatMode redSeatMode_ = SeatMode.SEAT_MODE_NONE;

    private final Map<String, PlayerInfo> observers_= new HashMap<String, PlayerInfo>();

    public TablePlayerTracker(TableType tableType) {
        tableType_ = tableType;
    }
    
    public void setUIViews(
            TextView blackLabel,
            Button blackButton,
            TextView redLabel,
            Button redButton) {
        
        blackLabel_ = blackLabel;
        blackButton_ = blackButton;
        redLabel_ = redLabel;
        redButton_ = redButton;
        hasUI_ = true;
    }

    public void unsetUIViews() {
        blackLabel_ = null;
        blackButton_ = null;
        redLabel_ = null;
        redButton_ = null;
        hasUI_ = false;
    }

    public void reverseView() {
        TextView view = blackLabel_;
        blackLabel_ = redLabel_;
        redLabel_ = view;
        
        Button button = blackButton_;
        blackButton_ = redButton_;
        redButton_ = button;
        
        syncUI();
    }
    
    public void setTableType(TableType tableType) {
        tableType_ = tableType;
    }
    
    //public TableType getTableType() {
    //    return tableType_;
    //}
    
    public void setBlackInfo(String pid, String rating) {
        blackPlayer_ = new PlayerInfo(pid, rating);
    }

    public void setRedInfo(String pid, String rating) {
        redPlayer_ = new PlayerInfo(pid, rating);
    }

    public void setObservers(List<String> observers) {
        observers_.clear();
        for (String pid : observers) {
            observers_.put(pid, new PlayerInfo(pid, "0"));
        }
    }

    public void clearAllPlayers()  {
        redPlayer_ = new PlayerInfo();
        blackPlayer_ = new PlayerInfo();
        observers_.clear();;
    }

    public PlayerInfo getRedPlayer() { return redPlayer_; }
    public PlayerInfo getBlackPlayer() { return blackPlayer_; }
    public Map<String, PlayerInfo> getObservers() { return observers_; }

    public boolean hasEnoughPlayers() {
        return blackPlayer_.isValid() && redPlayer_.isValid();
    }
    
    public void onPlayerLeave(String pid) {
        final String myPid = HoxApp.getApp().getMyPid();
        
        // Check if I just left the Table.
        if (myPid.equals(pid)) {
            // Do nothing here. The network controller will take care of this case.

        } else { // Other player left my table?
            if (blackPlayer_.hasPid(pid)) {
                blackPlayer_ = new PlayerInfo();
            } else if (redPlayer_.hasPid(pid)) {
                redPlayer_ = new PlayerInfo();
            } else {
                observers_.remove(pid);
            }
        }
    }
    
    public void onPlayerJoin(String pid, String rating, Enums.ColorEnum playerColor) {
        // Special case: The player left Red/Black seat.
        if (blackPlayer_.hasPid(pid)) {
            blackPlayer_ = new PlayerInfo();
        } else if (redPlayer_.hasPid(pid)) {
            redPlayer_ = new PlayerInfo();
        }
        
        // Assign the new seat.
        switch (playerColor) {
            case COLOR_BLACK:
                blackPlayer_ = new PlayerInfo(pid, rating);
                observers_.remove(pid);
                break;
            
            case COLOR_RED:
                redPlayer_ = new PlayerInfo(pid, rating);
                observers_.remove(pid);
                break;
            
            case COLOR_NONE:
                observers_.put(pid, new PlayerInfo(pid, rating));
                break;
            default:
                break; // Do nothing
        }
    }

    public void onPlayerRatingUpdate(String pid, String newRating) {
        if (blackPlayer_.hasPid(pid)) blackPlayer_.rating = newRating;
        else if (redPlayer_.hasPid(pid)) redPlayer_.rating = newRating;
        else {
            PlayerInfo foundObserver = observers_.get(pid);
            if (foundObserver != null) foundObserver.rating = newRating;
        }
    }

    private void syncSeatLabels() {
        if (!hasUI_) return;

        switch (tableType_) {
            case TABLE_TYPE_LOCAL:
            {
                final int aiLevel = HoxApp.getApp().getAILevel();
                if (myColor_ == ColorEnum.COLOR_RED) {
                    blackLabel_.setText(convertAILevelToString(aiLevel));
                    redLabel_.setText(HoxApp.getApp().getString(R.string.you_label));
                } else {
                    blackLabel_.setText(HoxApp.getApp().getString(R.string.you_label));
                    redLabel_.setText(convertAILevelToString(aiLevel));
                }
                break;
            }
            case TABLE_TYPE_NETWORK:
            {
                blackLabel_.setText(blackPlayer_.getInfo());
                redLabel_.setText(redPlayer_.getInfo());
                break;
            }
            case TABLE_TYPE_EMPTY:
                /* falls through */
            default:
            {
                blackLabel_.setText("");
                redLabel_.setText("");
                break;
            }
        }
    }

    private void syncSeatMode_Black() {
        if (!hasUI_) return;

        switch (blackSeatMode_) {
            case SEAT_MODE_PLAY:
                blackButton_.setVisibility(View.VISIBLE);
                blackButton_.setText(HoxApp.getApp().getString(R.string.button_play_black));
                break;
                
            case SEAT_MODE_LEAVE:
                blackButton_.setVisibility(View.VISIBLE);
                blackButton_.setText("X");
                break;
                
            case SEAT_MODE_NONE:
                /* falls through */
            default:
                blackButton_.setVisibility(View.INVISIBLE);
                break;
        }
    }
    
    private void syncSeatMode_Red() {
        if (!hasUI_) return;

        switch (redSeatMode_) {
            case SEAT_MODE_PLAY:
                redButton_.setVisibility(View.VISIBLE);
                redButton_.setText(HoxApp.getApp().getString(R.string.button_play_red));
                break;
                
            case SEAT_MODE_LEAVE:
                redButton_.setVisibility(View.VISIBLE);
                redButton_.setText("X");
                break;
                
            case SEAT_MODE_NONE:
                /* falls through */
            default:
                redButton_.setVisibility(View.INVISIBLE);
                break;
        }
    }
    
    private String convertAILevelToString(int aiLevel) {
        switch (aiLevel) {
            case 1: return HoxApp.getApp().getString(R.string.ai_label_medium);
            case 2: return HoxApp.getApp().getString(R.string.ai_label_difficult);
            case 0: /* falls through */
            default: return HoxApp.getApp().getString(R.string.ai_label_easy);
        }
    }
    
    public void syncUI() {
        Log.d(TAG, "Sync UI...: hasUI_=" + hasUI_ + ", tableType_=" + tableType_);

        syncSeatLabels();
        
        // Determine the seat modes.
        switch (tableType_) {
            case TABLE_TYPE_LOCAL:
            {
                blackSeatMode_ = SeatMode.SEAT_MODE_NONE;
                redSeatMode_ = SeatMode.SEAT_MODE_NONE;
                break;
            }
            case TABLE_TYPE_NETWORK:
            {
                final String myPid = HoxApp.getApp().getMyPid();
                boolean isGameStarted = (NetworkTableController.getInstance().getMoveCount() > 1);
                boolean isGameOver = HoxApp.getApp().isGameOver();
                
                if (blackPlayer_.hasPid(myPid)) { // I play BLACK?
                    if (isGameOver || (!isGameStarted)) {
                        blackSeatMode_ = SeatMode.SEAT_MODE_LEAVE;
                    } else {
                        blackSeatMode_ = SeatMode.SEAT_MODE_NONE;
                    }
                    redSeatMode_ = SeatMode.SEAT_MODE_NONE;
                    
                } else if (redPlayer_.hasPid(myPid)) { // I play RED?
                    if (isGameOver || (!isGameStarted)) {
                        redSeatMode_ = SeatMode.SEAT_MODE_LEAVE;
                    } else {
                        redSeatMode_ = SeatMode.SEAT_MODE_NONE;
                    }
                    blackSeatMode_ = SeatMode.SEAT_MODE_NONE;
                    
                } else {
                    blackSeatMode_ = (blackPlayer_.isValid() ? SeatMode.SEAT_MODE_NONE : SeatMode.SEAT_MODE_PLAY);
                    redSeatMode_ = (redPlayer_.isValid() ? SeatMode.SEAT_MODE_NONE : SeatMode.SEAT_MODE_PLAY);
                }
                
                break;
            }
            case TABLE_TYPE_EMPTY:
                /* falls through */
            default:
            {
                blackSeatMode_ = SeatMode.SEAT_MODE_NONE;
                redSeatMode_ = SeatMode.SEAT_MODE_NONE;
                break;
            }
        }
        
        syncSeatMode_Black();
        syncSeatMode_Red();
    }
    
}
