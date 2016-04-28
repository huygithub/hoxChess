package com.playxiangqi.hoxchess;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

public class PlayersFragment extends Fragment {

    private static final String TAG = "PlayersFragment";

    //private Context context_;

    private View inProgressView_;
    private ListView playersListView_;

    private PlayersAdapter adapter_;

    public PlayersFragment() {
        Log.d(TAG, "[CONSTRUCTOR]");
    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach(context);
//        context_ = context;
//    }

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

    private boolean refreshPlayersIfNeeded() {
        if (!PlayerManager.getInstance().arePlayersLoaded() ||
                !PlayerManager.getInstance().areTablesLoaded()) {
            Log.d(TAG, "refreshPlayersIfNeeded: Either player or table LIST is not yet loaded.");
            return false;
        }

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
        private final HashMap<Integer, PlayerInfo> mIdMap_ = new HashMap<Integer, PlayerInfo>();

        public PlayersAdapter(Activity context, int textViewResourceId) {
            activity_ = context;
            resourceId_ = textViewResourceId;
        }

        public void refreshPlayers() {
            mIdMap_.clear();
            HashMap<String, PlayerInfo> players = PlayerManager.getInstance().getPlayers();
            int position = 0;
            for (HashMap.Entry<String, PlayerInfo> entry : players.entrySet()) {
                mIdMap_.put(Integer.valueOf(position), entry.getValue());
                ++position;
            }

            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mIdMap_.size();
        }

        @Override
        public Object getItem(int position) {
            return mIdMap_.get(Integer.valueOf(position));
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

            final PlayerInfo playerInfo = (PlayerInfo) getItem(Integer.valueOf(position));
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
