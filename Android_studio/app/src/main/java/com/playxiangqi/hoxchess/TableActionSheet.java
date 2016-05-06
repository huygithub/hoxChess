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
import android.support.design.widget.BottomSheetDialog;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class TableActionSheet extends BottomSheetDialog {

    private static final String TAG = "TableActionSheet";

    public enum Action {
        ACTION_RESET_TABLE,
        ACTION_REVERSE_BOARD,
        ACTION_CLOSE_TABLE,
        ACTION_OFFER_DRAW,
        ACTION_OFFER_RESIGN,
        ACTION_NEW_TABLE,
    }

    private TextView headerTextView_;
    private View resetTableView_;
    private View reverseBoardView_;
    private View closeTableView_;
    private View offerDrawnView_;
    private View offerResignView_;
    private View newTableView_;

    /**
     * Constructor
     */
    public TableActionSheet(final Activity activity) {
        super(activity);

        View sheetView = activity.getLayoutInflater().inflate(R.layout.sheet_dialog_table_actions, null);
        setContentView(sheetView);

        headerTextView_ = (TextView) sheetView.findViewById(R.id.sheet_header_view);
        resetTableView_ = sheetView.findViewById(R.id.sheet_reset_table_view);
        reverseBoardView_ = sheetView.findViewById(R.id.sheet_reverse_table_view);
        closeTableView_ = sheetView.findViewById(R.id.sheet_close_table_view);
        offerDrawnView_ = sheetView.findViewById(R.id.sheet_offer_draw_view);
        offerResignView_ = sheetView.findViewById(R.id.sheet_offer_resign_view);
        newTableView_ = sheetView.findViewById(R.id.sheet_new_table_view);
    }

    public void setHeaderText(String headerText) {
        headerTextView_.setText(headerText);
    }

    public void hideAction(Action action) {
        switch (action) {
            case ACTION_RESET_TABLE:
                resetTableView_.setVisibility(View.GONE);
                break;
            case ACTION_REVERSE_BOARD:
                reverseBoardView_.setVisibility(View.GONE);
                break;
            case ACTION_CLOSE_TABLE:
                closeTableView_.setVisibility(View.GONE);
                break;
            case ACTION_OFFER_DRAW:
                offerDrawnView_.setVisibility(View.GONE);
                break;
            case ACTION_OFFER_RESIGN:
                offerResignView_.setVisibility(View.GONE);
                break;
            case ACTION_NEW_TABLE:
                newTableView_.setVisibility(View.GONE);
                break;
            default:
                Log.w(TAG, "hideAction: Unsupport action:" + action);
                break;
        }
    }

    public void setOnClickListener_ResetTable(View.OnClickListener listener) {
        resetTableView_.setOnClickListener(listener);
    }

    public void setOnClickListener_ReverseBoard(View.OnClickListener listener) {
        reverseBoardView_.setOnClickListener(listener);
    }

    public void setOnClickListener_Close(View.OnClickListener listener) {
        closeTableView_.setOnClickListener(listener);
    }

    public void setOnClickListener_OfferDrawn(View.OnClickListener listener) {
        offerDrawnView_.setOnClickListener(listener);
    }

    public void setOnClickListener_OfferResign(View.OnClickListener listener) {
        offerResignView_.setOnClickListener(listener);
    }

    public void setOnClickListener_NewTable(View.OnClickListener listener) {
        newTableView_.setOnClickListener(listener);
    }
}