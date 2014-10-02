/**
 *  Copyright 2014 Huy Phan <huyphan@playxiangqi.com>
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

import android.support.v7.app.ActionBarActivity;
import android.support.v4.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * The main (entry-point) activity.
 */
public class MainActivity extends ActionBarActivity implements HoxApp.SettingsObserver {

    private static final String TAG = "MainActivity";

    private Fragment placeholderFragment_;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        placeholderFragment_ = new PlaceholderFragment();
        
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, placeholderFragment_)
                    .commit();
        }
        
        HoxApp.getApp().registerSettingsObserver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            case R.id.action_search:
                Log.d(TAG, "Action 'New Table' clicked...");
                BoardView boardView = (BoardView) placeholderFragment_.getView().findViewById(R.id.board_view);
                if (boardView == null) {
                    Log.w(TAG, "The board view could not be found the placeholder fragment.");
                    return false;
                }
                boardView.onNewTableActionClicked();
                return true;
            case R.id.action_settings:
                Log.d(TAG, "Action 'Settings' clicked...");
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onAILevelChanged(int newLevel) {
        Log.d(TAG, "on AI Level changed. newLevel = " +  newLevel);
        updateAILevelOfBoard(newLevel);
        updateAILabel(newLevel);
    }

    private void updateAILevelOfBoard(int newLevel) {
        Log.d(TAG, "update AI Level of Board. newLevel = " +  newLevel);
        BoardView boardView = (BoardView) placeholderFragment_.getView().findViewById(R.id.board_view);
        if (boardView == null) {
            Log.w(TAG, "The board view could not be found the placeholder fragment.");
            return;
        }
        boardView.onAILevelChanged(newLevel);
    }
    
    private void updateAILabel(int aiLevel) {
        TextView aiLabel = (TextView) placeholderFragment_.getView().findViewById(R.id.ai_label);
        if (aiLabel == null) {
            Log.w(TAG, "The AI Label could not be found the placeholder fragment.");
            return;
        }
        String labelString;
        switch (aiLevel) {
            case 1: labelString = getString(R.string.ai_label_medium); break;
            case 2: labelString = getString(R.string.ai_label_difficult); break;
            case 0: /* falls through */
            default: labelString = getString(R.string.ai_label_easy);
        }
        aiLabel.setText(labelString);
    }
    
    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        private static final String TAG = "PlaceholderFragment";
        
        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            Log.d(TAG, "onCreateView...");
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
        
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            Log.d(TAG, "onActivityCreated...");
            
            int aiLevel = HoxApp.getApp().loadAILevelPreferences();
            ((MainActivity) getActivity()).updateAILabel(aiLevel);
            ((MainActivity) getActivity()).updateAILevelOfBoard(aiLevel);
        }
    }
}
