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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

public class TablesActivity extends AppCompatActivity
                implements TablesFragment.OnFragmentInteractionListener,
                           PlayersFragment.OnFragmentInteractionListener,
                           PlayerManager.EventListener {

    private static final String TAG = "TablesActivity";

    private View inProgressView_;
    private ViewPager viewPager_;

    private WeakReference<TablesFragment> myTablesFragment_ = new WeakReference<TablesFragment>(null);
    private WeakReference<PlayersFragment> myPlayersFragment_ = new WeakReference<PlayersFragment>(null);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tables);
        Log.d(TAG, "onCreate:");

        inProgressView_ = findViewById(R.id.tables_InProgressLayout);

        TablesPagerAdapter pagerAdapter = new TablesPagerAdapter(this, getSupportFragmentManager());
        viewPager_ = (ViewPager) findViewById(R.id.tables_view_pager);
        viewPager_.setAdapter(pagerAdapter);
    }

    @Override
    public void onPlayersLoaded() {
        Log.d(TAG, "onPlayersLoaded: Do nothing.");
    }

    @Override
    public void onTablesLoaded() {
        List<TableInfo> tables = PlayerManager.getInstance().getTables();
        Log.d(TAG, "onTablesLoaded: # of tables = " + tables.size());
        refreshTablesViewIfNeeded();
        refreshPlayersViewIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume:");
        if (!refreshTablesViewIfNeeded()) {
            PlayerManager.getInstance().addListener(this);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause:");
        PlayerManager.getInstance().removeListener(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "(ActionBar) onOptionsItemSelected");

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long);
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case android.R.id.home: // To handle the BACK button!
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean refreshTablesViewIfNeeded() {
        if (!PlayerManager.getInstance().areTablesLoaded()) {
            //Log.d(TAG, "refreshTablesViewIfNeeded: The table LIST is not yet loaded.");
            return false;
        }

        if (inProgressView_.getVisibility() != View.GONE) {
            inProgressView_.setVisibility(View.GONE);
            viewPager_.setVisibility(View.VISIBLE);
        }

        TablesFragment tablesFragment = myTablesFragment_.get();
        if (tablesFragment != null) {
            tablesFragment.refreshView();
        }

        return true;
    }

    private boolean refreshPlayersViewIfNeeded() {
        if (!PlayerManager.getInstance().areTablesLoaded()) {
            //Log.d(TAG, "refreshPlayersViewIfNeeded: The table LIST is not yet loaded.");
            return false;
        }

        if (inProgressView_.getVisibility() != View.GONE) {
            inProgressView_.setVisibility(View.GONE);
            viewPager_.setVisibility(View.VISIBLE);
        }

        PlayersFragment playersFragment = myPlayersFragment_.get();
        if (playersFragment != null) {
            playersFragment.refreshPlayersIfNeeded();
        }

        return true;
    }

    /**
     * Implements the interface TablesFragment.OnFragmentInteractionListener
     */
    @Override
    public void onTablesFragment_CreateView(TablesFragment fragment) {
        myTablesFragment_ = new WeakReference<TablesFragment>(fragment);
        refreshTablesViewIfNeeded();
    }

    /**
     * Implements the interface TablesFragment.OnFragmentInteractionListener
     */
    @Override
    public void onTablesFragment_DestroyView(TablesFragment fragment) {
        TablesFragment tablesFragment = myTablesFragment_.get();
        if (tablesFragment != null && tablesFragment == fragment) {
            Log.d(TAG, "Tables fragment view destroyed. Release weak reference.");
            myTablesFragment_ = new WeakReference<TablesFragment>(null);
        }
    }

    /**
     * Implements the interface TablesFragment.OnFragmentInteractionListener
     */
    @Override
    public void onTableSelected(String tableId) {
        Log.d(TAG, "onTableSelected: tableId = " + tableId);

        // Return the table-ID to the caller.
        Intent result = new Intent();
        result.putExtra("tid", tableId);
        setResult(Activity.RESULT_OK, result);
        finish();
    }

    /**
     * Implementation of PlayersFragment.OnFragmentInteractionListener
     */
    @Override
    public void onPlayersFragment_CreateView(PlayersFragment fragment) {
        myPlayersFragment_ = new WeakReference<PlayersFragment>(fragment);
        refreshPlayersViewIfNeeded();
    }

    /**
     * Implementation of PlayersFragment.OnFragmentInteractionListener
     */
    @Override
    public void onPlayersFragment_DestroyView(PlayersFragment fragment) {
        PlayersFragment playersFragment = myPlayersFragment_.get();
        if (playersFragment != null && playersFragment == fragment) {
            myPlayersFragment_ = new WeakReference<PlayersFragment>(null);
            Log.d(TAG, "Release Players fragment: " + playersFragment);
        }
    }

    /**
     * Implementation of PlayersFragment.OnFragmentInteractionListener
     */
    @Override
    public List<PlayerInfo> onRequestToRefreshPlayers() {
        List<PlayerInfo> players = new ArrayList<PlayerInfo>();

        HashMap<String, PlayerInfo> playersMap = PlayerManager.getInstance().getPlayers();
        for (HashMap.Entry<String, PlayerInfo> entry : playersMap.entrySet()) {
            players.add(entry.getValue());
        }

        return players;
    }

    /**
     * Implementation of PlayersFragment.OnFragmentInteractionListener
     */
    @Override
    public void onPlayerClick(PlayerInfo playerInfo, final String tableId) {
        final String playerId = playerInfo.pid;
        final Activity activity = this;
        final PlayerActionSheet dialog = new PlayerActionSheet(this, playerInfo);

        // Setup for "Invite" or "Join".
        if (TextUtils.isEmpty(tableId)) {
            dialog.hideAction(PlayerActionSheet.Action.ACTION_JOIN_TABLE);
        } else {
            dialog.hideAction(PlayerActionSheet.Action.ACTION_INVITE_PLAYER);
        }

        dialog.setActionListener(new PlayerActionSheet.ActionListener() {
            @Override
            public void onActionClick(PlayerActionSheet.Action action) {
                if (action == PlayerActionSheet.Action.ACTION_SEND_MESSAGE) {
                    PlayerActionSheet.sendMessageToPlayer(activity, playerId);
                } else if (action == PlayerActionSheet.Action.ACTION_INVITE_PLAYER) {
                    HoxApp.getApp().getNetworkController().handleRequestToInvite(playerId);
                } else if (action == PlayerActionSheet.Action.ACTION_JOIN_TABLE) {
                    onTableSelected(tableId);
                }
            }
        });

        dialog.show();
    }

    /**
     * The ViewPager adapter for the tables activity.
     */
    private static class TablesPagerAdapter extends FragmentPagerAdapter {
        private final Context context_;

        public TablesPagerAdapter(Context context, FragmentManager fragmentManager) {
            super(fragmentManager);
            context_ = context;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0: return TablesFragment.newInstance();
                default: return new PlayersFragment();
            }
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0: return context_.getString(R.string.label_table);
                default: return context_.getString(R.string.action_view_players);
            }
        }

        /**
         * Reference: https://guides.codepath.com/android/ViewPager-with-FragmentPagerAdapter
         */
        @Override
        public float getPageWidth (int position) {
            switch (position) {
                case 0: return 1f;
                default: return 1f;
            }
        }
    }
}
