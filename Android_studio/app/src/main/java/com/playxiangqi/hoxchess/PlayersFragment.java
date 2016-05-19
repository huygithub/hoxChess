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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class PlayersFragment extends Fragment {

    private static final String TAG = "PlayersFragment";
    private boolean DEBUG_LIFE_CYCLE = true;

    private OnFragmentInteractionListener listener_;

    private View inProgressView_;
    private ListView playersListView_;

    private PlayersAdapter adapter_;

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
        void onPlayersFragment_CreateView(PlayersFragment fragment);
        void onPlayersFragment_DestroyView(PlayersFragment fragment);
        List<PlayerInfo> onRequestToRefreshPlayers();
        void onPlayerClick(PlayerInfo playerInfo, final String tableId);
    }

    public PlayersFragment() {
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "[CONSTRUCTOR]");
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onCreateView...");
        final View view = inflater.inflate(R.layout.fragment_players_in_table, container, false);

        inProgressView_ = view.findViewById(R.id.inProgressLayout);
        playersListView_ = (ListView)view.findViewById(R.id.list_players);

        adapter_ = new PlayersAdapter(getActivity(), R.layout.listview_item_player);
        playersListView_.setAdapter(adapter_);

        playersListView_.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlayerInfo playerInfo = (PlayerInfo) playersListView_.getItemAtPosition(position);
                Log.d(TAG, "onItemClick: View:" + view + ", Position:" + position + " pid: " + playerInfo.pid
                        + ", ListItem: " + playerInfo);

                // Note: I don't know of a better way to get table ID.
                String tableId = null;
                ViewHolder holder = (ViewHolder) view.getTag();
                if (holder != null) {
                    tableId = holder.tableIdView.getText().toString();
                    Log.d(TAG, "onItemClick: tableId = [" + tableId + "]");
                }

                handlePlayerClickEvent(playerInfo, tableId);
            }
        });

        // Empty initially.
        inProgressView_.setVisibility(View.GONE);
        playersListView_.setVisibility(View.VISIBLE);

        listener_.onPlayersFragment_CreateView(this);
        return view;
    }

    private void handlePlayerClickEvent(PlayerInfo playerInfo, String tableId) {
        listener_.onPlayerClick(playerInfo, tableId);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onActivityCreated...");
    }

    @Override
    public void onResume () {
        super.onResume();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onResume...");

        if (!refreshPlayersIfNeeded()) {
            //PlayerManager.getInstance().addListener(this);
        }
    }

    @Override
    public void onPause () {
        super.onPause();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onPause...");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDestroyView");
        listener_.onPlayersFragment_DestroyView(this);
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

        List<PlayerInfo> players = listener_.onRequestToRefreshPlayers();
        adapter_.refreshPlayers(players);
        return true;
    }

    @Override
    public void onDestroy () {
        super.onDestroy();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDestroy...");
    }

    @Override
    public void onDetach () {
        super.onDetach();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDetach...");
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

        public void refreshPlayers(List<PlayerInfo> newPlayers) {
            players_.clear();
            players_.addAll(newPlayers);
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

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final PlayerInfo playerInfo = (PlayerInfo) getItem(position);
            holder.playerIdView.setText(playerInfo.pid);
            holder.playerRatingView.setText(playerInfo.rating);

            final String playerTable = PlayerManager.getInstance().findTableOfPlayer(playerInfo.pid);
            holder.tableIdView.setText(TextUtils.isEmpty(playerTable) ? "" : playerTable);

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
    }
}
