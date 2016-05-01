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

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link BoardFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link BoardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class BoardFragment extends Fragment {

    private static final String TAG = "BoardFragment";

    private boolean DEBUG_LIFE_CYCLE = true;

    // Parameter arguments.
    private static final String ARG_BOARD_TYPE = "board_type";

    // Types of parameters
    private String boardType_;

    private OnFragmentInteractionListener listener_;

    private BoardView boardView_;

    private boolean isBlackOnTop_ = true; // Normal view. Black player is at the top position.

    private static final String STATE_IS_BLACK_ON_TOP = "isBlackOnTop";

    /**
     * Constructor
     */
    public BoardFragment() {
        // Required empty public constructor
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "[CONSTRUCTOR]");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param boardType The type of board (Empty, AI, or Network)
     * @return A new instance of fragment BoardFragment.
     */
    public static BoardFragment newInstance(String boardType) {
        BoardFragment fragment = new BoardFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BOARD_TYPE, boardType);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onAttach");
        if (context instanceof OnFragmentInteractionListener) {
            listener_ = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }

        listener_.onBoardFragment_Attach(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onCreate");
        if (getArguments() != null) {
            boardType_ = getArguments().getString(ARG_BOARD_TYPE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_LIFE_CYCLE) Log.d(TAG, "onCreateView: board-type = " + boardType_ );

        //TextView textView = new TextView(getActivity());
        //textView.setText(R.string.hello_blank_fragment);

        final View view = inflater.inflate(R.layout.fragment_main, container, false);

        // ********** START of MainActivity.onBoardViewCreated **************
        // (from MainActivity, already disabled!) progressBar_ = (ProgressBar) findViewById(R.id.progress_spinner);

        boardView_ = (BoardView) view.findViewById(R.id.board_view);
        TextView topPlayerLabel = (TextView) view.findViewById(R.id.top_player_label);
        TextView bottomPlayerLabel = (TextView) view.findViewById(R.id.bottom_player_label);
        Button topPlayerButton = (Button) view.findViewById(R.id.top_button);
        Button bottomPlayerButton = (Button) view.findViewById(R.id.bottom_button);

        // (Done. Using callback) boardView_.setBoardEventListener(tableController_);

        // Game timers.
        TextView topGameTimeView = (TextView) view.findViewById(R.id.top_game_time);
        TextView topMoveTimeView = (TextView) view.findViewById(R.id.top_move_time);
        TextView bottomGameTimeView = (TextView) view.findViewById(R.id.bottom_game_time);
        TextView bottomMoveTimeView = (TextView) view.findViewById(R.id.bottom_move_time);

        TableTimeTracker timeTracker = HoxApp.getApp().getTimeTracker();
        timeTracker.setUITextViews(
                topGameTimeView, topMoveTimeView, bottomGameTimeView, bottomMoveTimeView);
        timeTracker.reset();

        // Player tracker.
        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.setUIViews(
                topPlayerLabel, topPlayerButton, bottomPlayerLabel, bottomPlayerButton);

        // Restore the previous state of the board.
        List<Piece.Move> historyMoves = HoxApp.getApp().getReferee().getHistoryMoves();
        int moveCount = historyMoves.size();
        int moveIndex = 0;
        for (Piece.Move move : historyMoves) {
            Log.d(TAG, "Update board with a new AI move = " + move.fromPosition + " => " + move.toPosition);
            final boolean isLastMove = (moveIndex == (moveCount - 1));
            boardView_.restoreMove(move.fromPosition, move.toPosition, isLastMove);
            ++moveIndex;
        }

        if (HoxApp.getApp().isGameOver()) {
            final Enums.GameStatus gameStatus = HoxApp.getApp().getGameStatus();
            Log.d(TAG, "... Game Over: gameStatus = " + gameStatus);
            boardView_.onGameEnded(gameStatus);
        }

        boardView_.invalidate();

        // Set table Id.
        // (Don in callback) tableController_.setTableTitle();

        // (We do it in MainActivity.onCreate) SoundManager.getInstance().initialize(activity);
        // **************** END of MainActivity.onBoardViewCreated **********************

        setOnClickHandlers(view);

        listener_.onBoardFragment_CreateView();

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onActivityCreated");

        if (savedInstanceState != null) {
            boolean isBlackOnTop = savedInstanceState.getBoolean(STATE_IS_BLACK_ON_TOP, isBlackOnTop_);
            if (!isBlackOnTop) {
                reverseView();
            }
        }
    }

    @Override
    public void onResume () {
        super.onResume();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onResume");

        TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();
        playerTracker.syncUI(); // Among things to be updated, AI Level is one.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDestroyView");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDestroy");
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDetach");
        listener_ = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onSaveInstanceState");

        // Save the table's current game state
        outState.putBoolean(STATE_IS_BLACK_ON_TOP, isBlackOnTop_);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onBoardFragment_Attach(BoardFragment boardFragment);
        void onBoardFragment_CreateView();
        void onResetViewClick(View v);
        void onChangeRoleRequest(Enums.ColorEnum clickedColor);
    }

    // ***************************************************************************
    //
    //         Public APIs to be called by the main activity
    //
    // ***************************************************************************

    public void setBoardEventListener(BoardView.BoardEventListener listener) {
        if (boardView_ != null) {
            boardView_.setBoardEventListener(listener);
        }
    }

    public void makeMove(final Position fromPos, final Position toPos, boolean animated) {
        boardView_.makeMove(fromPos, toPos, animated);
    }

    public void resetBoard() {
        boardView_.resetBoard();
    }

    public void resetBoardWithNewMoves(String[] moves) {
        Log.d(TAG, "Reset board with new (MOVES): length = " + moves.length);
        for (String move : moves) {
            int row1 = move.charAt(1) - '0';
            int col1 = move.charAt(0) - '0';
            int row2 = move.charAt(3) - '0';
            int col2 = move.charAt(2) - '0';
            boardView_.makeMove(new Position(row1, col1), new Position(row2, col2), false);
        }
        boardView_.invalidate();
    }

    public void onGameEnded(Enums.GameStatus gameStatus) {
        boardView_.onGameEnded(gameStatus);
    }

    public void reverseView() {
        Log.d(TAG, "Reverse board view...");
        isBlackOnTop_ = !isBlackOnTop_;
        boardView_.reverseView();
        HoxApp.getApp().getTimeTracker().reverseView();
        HoxApp.getApp().getPlayerTracker().reverseView();
    }

    public void onLocalPlayerJoined(Enums.ColorEnum myColor) {
        // Reverse the board view so that my seat is always at the bottom of the screen.
        if (  (myColor == Enums.ColorEnum.COLOR_RED && !isBlackOnTop_) ||
                (myColor == Enums.ColorEnum.COLOR_BLACK && isBlackOnTop_) ) {
            reverseView();
        }
    }

    // ***************************************************************************
    //
    //         Private APIs
    //
    // ***************************************************************************

    private void setOnClickHandlers(View view) {
        ImageButton reverseViewButton = (ImageButton) view.findViewById(R.id.reverse_view);
        if (reverseViewButton != null) {
            reverseViewButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    reverseView();
                }
            });
        }

        ImageButton resetButton = (ImageButton) view.findViewById(R.id.action_reset);
        if (resetButton != null) {
            resetButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener_ != null) {
                        listener_.onResetViewClick(v);
                    }
                }
            });
        }

        Button topPlayerButton = (Button) view.findViewById(R.id.top_button);
        if (topPlayerButton != null) {
            topPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Enums.ColorEnum clickedColor =
                            (isBlackOnTop_ ? Enums.ColorEnum.COLOR_BLACK : Enums.ColorEnum.COLOR_RED);
                    if (listener_ != null) {
                        listener_.onChangeRoleRequest(clickedColor);
                    }
                }
            });
        }

        Button bottomPlayerButton = (Button) view.findViewById(R.id.bottom_button);
        if (bottomPlayerButton != null) {
            bottomPlayerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Enums.ColorEnum clickedColor =
                            (isBlackOnTop_ ? Enums.ColorEnum.COLOR_RED : Enums.ColorEnum.COLOR_BLACK);
                    if (listener_ != null) {
                        listener_.onChangeRoleRequest(clickedColor);
                    }
                }
            });
        }

        // Setup the long-click handlers to handle BEGIN and END actions of replay.
        ImageButton previousButton = (ImageButton) view.findViewById(R.id.replay_previous);
        if (previousButton != null) {
            previousButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boardView_.onReplay_PREV(true);
                }
            });

            previousButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    boardView_.onReplay_BEGIN();
                    return true;
                }
            });
        }

        ImageButton nextButton = (ImageButton) view.findViewById(R.id.replay_next);
        if (nextButton != null) {
            nextButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boardView_.onReplay_NEXT(true);
                }
            });

            nextButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    boardView_.onReplay_END();
                    return true;
                }
            });
        }
    }

}
