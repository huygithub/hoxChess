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
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PlayersFragment extends Fragment {

    private static final String TAG = "PlayersFragment";

    private View inProgressView_;
    private ListView playersListView_;

    private PlayersAdapter adapter_;

    public PlayersFragment() {
        Log.d(TAG, "[CONSTRUCTOR]");
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) {
            Log.d(TAG, "onAttach: context = MainActivity. Register self with the activity.");
            MainActivity activity = (MainActivity) context;
            if (activity != null) {
                activity.registerPlayersFragment(this);
            }
        } else {
            Log.d(TAG, "onAttach: context != MainActivity. Do not register.");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView...");
        final View view = inflater.inflate(R.layout.fragment_players_in_table, container, false);

        inProgressView_ = view.findViewById(R.id.inProgressLayout);
        playersListView_ = (ListView)view.findViewById(R.id.list_players);

        adapter_ = new PlayersAdapter(getActivity(), R.layout.listview_item_player);
        playersListView_.setAdapter(adapter_);

        playersListView_.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlayerInfo itemValue = (PlayerInfo) playersListView_.getItemAtPosition(position);
                Log.d(TAG, "Position:" + position + " pid: " + itemValue.pid
                        + ", ListItem: " + itemValue);
                // Do nothing currently!
            }
        });

        // Empty initially.
        inProgressView_.setVisibility(View.GONE);
        playersListView_.setVisibility(View.VISIBLE);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated...");

        //MainActivity activity = (MainActivity) getActivity();
        //activity.onBoardViewCreated(activity);
    }

    @Override
    public void onResume () {
        super.onResume();
        Log.d(TAG, "onResume...");

        if (!refreshPlayersIfNeeded()) {
            //PlayerManager.getInstance().addListener(this);
        }
    }

    @Override
    public void onPause () {
        super.onPause();
        Log.d(TAG, "onPause...");
        //MainActivity activity = (MainActivity) getActivity();
        //activity.onBoardViewResume(activity);
    }

    public void clearAll() {
        adapter_.clearAll();
    }

    public void onPlayerJoin(String pid, String rating, Enums.ColorEnum playerColor) {
        adapter_.addPlayer(pid, rating);
    }

    public void onPlayerLeave(String pid) {
        adapter_.removePlayer(pid);
    }

    public boolean refreshPlayersIfNeeded() {
        if (inProgressView_.getVisibility() != View.GONE) {
            inProgressView_.setVisibility(View.GONE);
            playersListView_.setVisibility(View.VISIBLE);
        }
        adapter_.refreshPlayers();
        return true;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        Log.d(TAG, "onDestroy...");
        //HoxApp.getApp().registerChatActivity(null);
    }

    @Override
    public void onDetach () {
        super.onDetach();
        Log.d(TAG, "onDetach...");
    }

    /**
     * The custom adapter for our list view.
     */
    private static class PlayersAdapter extends BaseAdapter {
        private final Activity activity_;
        private final int resourceId_;
        private final List<PlayerInfo> players_ = new ArrayList<PlayerInfo>();

        public PlayersAdapter(Activity context, int textViewResourceId) {
            activity_ = context;
            resourceId_ = textViewResourceId;
        }

        public void refreshPlayers() {
            players_.clear();

            TablePlayerTracker playerTracker = HoxApp.getApp().getPlayerTracker();

            PlayerInfo redPlayer = playerTracker.getRedPlayer();
            if (redPlayer.isValid()) {
                players_.add(redPlayer);
            }

            PlayerInfo blackPlayer = playerTracker.getBlackPlayer();
            if (blackPlayer.isValid()) {
                players_.add(blackPlayer);
            }

            Map<String, PlayerInfo> observers = playerTracker.getObservers();
            for (HashMap.Entry<String, PlayerInfo> entry : observers.entrySet()) {
                players_.add(entry.getValue());
            }

            notifyDataSetChanged();
        }

        public void clearAll() {
            if (!players_.isEmpty()) {
                players_.clear();
                notifyDataSetChanged();
            }
        }

        public void addPlayer(String pid, String rating) {
            PlayerInfo foundPlayer = null;
            for (PlayerInfo player : players_) {
                if (player.hasPid(pid)) {
                    foundPlayer = player;
                    break;
                }
            }

            if (foundPlayer != null) {
                foundPlayer.rating = rating;
            } else {
                players_.add(new PlayerInfo(pid, rating));
            }
            notifyDataSetChanged();
        }

        public void removePlayer(String pid) {
            Iterator<PlayerInfo> iterator = players_.iterator();
            while (iterator.hasNext()) {
                PlayerInfo player = iterator.next();
                if (player.hasPid(pid)) {
                    iterator.remove();
                    notifyDataSetChanged();
                    break;
                }
            }
        }

        @Override
        public int getCount() {
            //Log.d(TAG, "getCount: count = " + players_.size());
            return players_.size();
        }

        @Override
        public Object getItem(int position) {
            return players_.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //Log.d(TAG, "getView: position = " + position + ", count = " + getCount());
            ViewHolder holder;
            if (convertView == null) {
                LayoutInflater layoutInflater = activity_.getLayoutInflater();
                convertView = layoutInflater.inflate(resourceId_, null);
                holder = new ViewHolder();
                holder.playerIdView = (TextView) convertView.findViewById(R.id.player_id);
                holder.playerRatingView = (TextView) convertView.findViewById(R.id.player_rating);
                holder.tableIdView = (TextView) convertView.findViewById(R.id.table_id);
                holder.menuImageView = (ImageView) convertView.findViewById(R.id.player_action_menu);

                holder.menuImageView.setOnClickListener(new ContextMenuOnClickListener(activity_));

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final PlayerInfo playerInfo = (PlayerInfo) getItem(position);
            holder.playerIdView.setText(playerInfo.pid);
            holder.playerRatingView.setText(playerInfo.rating);

            final String playerTable = PlayerManager.getInstance().findTableOfPlayer(playerInfo.pid);
            holder.tableIdView.setText(TextUtils.isEmpty(playerTable) ? "" : playerTable);

            holder.menuImageView.setTag(holder);

            return convertView;
        }

    }

    /**
     * The view holder for our custom adapter.
     */
    private static class ViewHolder {
        public TextView playerIdView;
        public TextView playerRatingView;
        public TextView tableIdView;
        public ImageView menuImageView;
    }

    private static class ContextMenuOnClickListener implements View.OnClickListener {

        private final Activity activity_;

        ContextMenuOnClickListener(Activity activity) {
            activity_ = activity;
        }

        @Override
        public void onClick(View view) {
            ViewHolder holder = (ViewHolder) view.getTag();
            final String playerId = holder.playerIdView.getText().toString();
            final String tableId = holder.tableIdView.getText().toString();
            Log.d(TAG, "(OnClickListener): playerId:" + playerId + ", tableId:" + tableId);

            PopupMenu popup = new PopupMenu(view.getContext(), view);
            popup.getMenuInflater().inflate(R.menu.players_activity_actions, popup.getMenu());

            if (TextUtils.isEmpty(tableId)) {
                popup.getMenu().removeItem(R.id.action_join_table);
            } else {
                popup.getMenu().removeItem(R.id.action_invite_to_play);
            }

            if (popup.getMenu().size() == 0) {
                Log.i(TAG, "(OnClickListener): No need to show popup menu!");
                return;
            }

            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_join_table:
                            HoxApp.getApp().getNetworkController().handleTableSelection(tableId);
                            break;
                        case R.id.action_invite_to_play:
                            HoxApp.getApp().getNetworkController().handleRequestToInvite(playerId);
                            break;
                        default:
                            return true;
                    }

                    Intent result = new Intent();
                    result.putExtra("pid", playerId); // NOTE: Not used currently!
                    activity_.setResult(Activity.RESULT_OK, result);
                    activity_.finish();
                    return true;
                }
            });

            popup.show();

        }
    }
}
