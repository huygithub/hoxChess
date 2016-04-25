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

import java.util.HashMap;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class PlayersActivity extends Activity {

    private static final String TAG = "PlayersActivity";
    
    private ListView playersListView_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_players);
        
        Log.d(TAG, "onCreate:");
        
        playersListView_ = (ListView)findViewById(R.id.list_players);
        
        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                R.layout.listview_item_player);
        playersListView_.setAdapter(adapter);

        playersListView_.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlayerInfo itemValue = (PlayerInfo) playersListView_.getItemAtPosition(position);
                
                Log.d(TAG, "Position:" + position + " pid: " + itemValue.pid
                        + ", ListItem: " + itemValue);
                
                // Return the player-ID.
                Intent result = new Intent();
                result.putExtra("pid", itemValue.pid);
                setResult(Activity.RESULT_OK, result);
                finish();
            }
        });
        
    }

    /**
     * The custom adapter for our list view.
     */
    private static class StableArrayAdapter extends BaseAdapter {
        private final Activity activity_;
        private final int resourceId_;
        private final HashMap<Integer, PlayerInfo> mIdMap_ = new HashMap<Integer, PlayerInfo>();

        public StableArrayAdapter(Activity context, int textViewResourceId) {
            activity_ = context;
            resourceId_ = textViewResourceId;

            HashMap<String, PlayerInfo> players = PlayerManager.getInstance().getPlayers();
            int position = 0;
            for (HashMap.Entry<String, PlayerInfo> entry : players.entrySet()) {
                mIdMap_.put(Integer.valueOf(position), entry.getValue());
                ++position;
            }
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
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final PlayerInfo playerInfo = (PlayerInfo) getItem(Integer.valueOf(position));
            holder.playerIdView.setText(playerInfo.pid);
            holder.playerRatingView.setText(playerInfo.rating);

            return convertView;
        }

    }

    /**
     * The view holder for our custom adapter.
     */
    private static class ViewHolder {
        public TextView playerIdView;
        public TextView playerRatingView;
    }
    
}
