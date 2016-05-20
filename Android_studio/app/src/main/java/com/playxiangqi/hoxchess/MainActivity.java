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
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

/**
 * The main (entry-point) activity.
 */
public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
                MessageManager.EventListener,
                NetworkController.NetworkEventListener,
                HomeFragment.OnHomeFragmentListener {

    private static final String TAG = "MainActivity";

    // The request codes
    private static final int CHANGE_SETTINGS_REQUEST = 1;

    private DrawerLayout drawerLayout_;
    private ActionBarDrawerToggle drawerToggle_;

    private MenuItem notificationMenuItem_;

    private SettingsActivity.SettingsInfo settingsInfo_;

     // A handler object, used for deferring UI operations.
    private Handler handler_ = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(toolbar);

        setupDrawer(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        } else {
            Log.w(TAG, "onCreate: getSupportActionBar() = null. Do not set Display options!");
        }

        Log.d(TAG, "onCreate: savedInstanceState = " + savedInstanceState + ".");

        // Check that the activity is using the layout version with
        // the main_container FrameLayout
        if (findViewById(R.id.main_container) != null) {
            // However, if we're being restored from a previous state,
            // then we don't want to create another fragment, which would create the problem
            // of overlapping fragments.
            if (savedInstanceState == null) {
                HomeFragment homeFragment = HomeFragment.newInstance();
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.main_container, homeFragment).commit();
            }
        }

        // NOTE: It is important to control our App 's audio volume using the Hardware Control Keys.
        // Reference:
        //    http://developer.android.com/training/managing-audio/volume-playback.html
        setVolumeControlStream(SoundManager.getInstance().getStreamType());

        SoundManager.getInstance().initialize(this);

        // Auto login the server.
        handler_.post(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "In UI thread again (onCreate): Login to server...");
                HoxApp.getApp().loginServer();
            }
        });
    }

    private void setupDrawer(Toolbar toolbar) {
        drawerLayout_ = (DrawerLayout) findViewById(R.id.drawer_layout);

        drawerToggle_ = new ActionBarDrawerToggle(this, drawerLayout_, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close) {

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                handleEvent_onDrawerOpened(drawerView);
            }

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                Log.d(TAG, "onDrawerClosed");
            }
        };

        drawerLayout_.addDrawerListener(drawerToggle_);
        drawerToggle_.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View headerView = navigationView.getHeaderView(0);
        headerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Navigation header view: onClick");
                openSettingsView();
                drawerLayout_.closeDrawer(GravityCompat.START);
            }
        });

        drawerToggle_.setDrawerIndicatorEnabled(true);
    }

    private void handleEvent_onDrawerOpened(View drawerView) {
        Log.d(TAG, "Handle event: onDrawerOpened");

        // Update header items.
        TextView playerNameView = (TextView) drawerView.findViewById(R.id.textview_player_name);
        if (playerNameView == null) { // Sanity check. This should not happen!
            Log.e(TAG, "onDrawerOpened: Player Name TextView not found!!!");
            return;
        }
        TextView playerIdView = (TextView) drawerView.findViewById(R.id.textview_player_id);
        if (playerIdView == null) { // Sanity check. This should not happen!
            Log.e(TAG, "onDrawerOpened: Player ID TextView not found!!!");
            return;
        }

        String playerName;
        String playerID;

        if (SettingsActivity.getLoginWithAccountFlag(this)) {
            playerName = getString(R.string.playxiangqi_account);
            playerID = SettingsActivity.getAccountPid(this);
        } else { // Login as Guest
            playerName = getString(R.string.guest_account_name);
            playerID = HoxApp.getApp().getMyPid(); // currentGuestId
            if (TextUtils.isEmpty(playerID)) {
                playerID = getString(R.string.guest_id_default);
            }
        }
        playerNameView.setText(playerName);
        playerIdView.setText(playerID);

        // Update sub-header items.
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        MenuItem logoutItem = navigationView.getMenu().findItem(R.id.action_logout);
        if (logoutItem != null) {
            //logoutItem.setVisible(HoxApp.getApp().isOnline());
            logoutItem.setTitle(HoxApp.getApp().isOnline() ? R.string.action_logout : R.string.action_login);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Log.d(TAG, "onPostCreate");
        drawerToggle_.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        drawerToggle_.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            //case R.id.action_view_tables:
            //    onViewTablesClicked();
            //    break;
            case R.id.action_settings:
                openSettingsView();
                break;
            case R.id.action_about: {
                Intent intent = new Intent(this, AboutActivity.class);
                startActivity(intent);
                break;
            }
            case R.id.action_www_playxiangqi: {
                openWebsiteToServer();
                break;
            }
            case R.id.action_logout:
                if (HoxApp.getApp().isOnline()) {
                    NetworkController.getInstance().logoutFromNetwork();
                } else {
                    HoxApp.getApp().loginServer();
                }
                break;
            default:
                break;
        }

        drawerLayout_.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (notificationMenuItem_ != null) {
            BadgeDrawable.setBadgeCount(this, notificationMenuItem_,
                    MessageManager.getInstance().getNotificationCount());
        }
        MessageManager.getInstance().addListener(this);

        NetworkController.getInstance().addListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        MessageManager.getInstance().removeListener(this);
        NetworkController.getInstance().removeListener(this);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "(ActionBar) onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main_activity_actions, menu);

        // Set up the notification item.
        notificationMenuItem_ = menu.findItem(R.id.action_notifications);
        BadgeDrawable.setBadgeCount(this, notificationMenuItem_,
                MessageManager.getInstance().getNotificationCount());

        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "(ActionBar) onPrepareOptionsMenu");
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.action_new_table).setVisible(false);
        menu.findItem(R.id.action_close_table).setVisible(false);
        menu.findItem(R.id.action_view_tables).setVisible(false);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "(ActionBar) onOptionsItemSelected");

        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (drawerToggle_.onOptionsItemSelected(item)) {
            return true;
        }
        // Handle your other action bar items...

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            //case android.R.id.home:
            //    tableController_.handleRequestToCloseCurrentTable();
            //    return true;
            case R.id.action_notifications:
                openNotificationView();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void openSettingsView() {
        Log.d(TAG, "Open 'Settings' view...");
        // Make a copy of the current settings.
        settingsInfo_ = SettingsActivity.getCurrentSettingsInfo(this);

        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, CHANGE_SETTINGS_REQUEST);
    }

    private void openWebsiteToServer() {
        final String url = Enums.HC_URL_SERVER;
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        // Note: To avoid being crashed if there is no application that can handle this
        //  implicit intent, verify that the intent will resolve to an activity.
        //    + resolveActivity() returns null if none are registered.
        //
        // Reference:
        //    http://developer.android.com/guide/components/intents-filters.html#ExampleSend
        //
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.w(TAG, "There is no activity that can visit this URL : " + url);
        }
    }

    private void openNotificationView() {
        Log.d(TAG, "Open 'Notification' view...");
        startActivity(new Intent(this, NotificationActivity.class));
        invalidateOptionsMenu(); // // Recreate the options menu
    }

    @Override
    public void onMessageReceived(MessageInfo messageInfo) {
        Log.d(TAG, "On new message: {#" + messageInfo.getId() + " " + messageInfo + "}");
        if (MessageManager.getInstance().isNotificationType(messageInfo.type)) {
            invalidateOptionsMenu(); // Recreate the options menu
        }
    }

    public void showBriefMessage(int resId, int duration) {
        View view = findViewById(R.id.main_container);
        Snackbar.make(view, resId, duration).show();
    }

    public void showBriefMessage(CharSequence text, int duration) {
        View view = findViewById(R.id.main_container);
        Snackbar.make(view, text, duration).show();
    }

    @Override
    public void onLoginSuccess() {
        Log.d(TAG, "onLoginSuccess:...");
        showBriefMessage(R.string.msg_login_success, Snackbar.LENGTH_SHORT);
    }

    @Override
    public void onLogout() {
        Log.d(TAG, "onLogout:...");
        showBriefMessage(R.string.msg_logout_done, Snackbar.LENGTH_SHORT);
    }

    @Override
    public void onLoginFailure(int errorMessageResId) {
        Log.d(TAG, "onLoginFailure:...");
        showBriefMessage(errorMessageResId, Snackbar.LENGTH_LONG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult:...");
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == CHANGE_SETTINGS_REQUEST) {
            Log.d(TAG, "onActivityResult: on Settings changed...");
            SettingsActivity.SettingsInfo newSettings = SettingsActivity.getCurrentSettingsInfo(this);
            if (newSettings.loginWithAccount != settingsInfo_.loginWithAccount
                    || !TextUtils.equals(newSettings.myPid, settingsInfo_.myPid)
                    || !TextUtils.equals(newSettings.myPassword, settingsInfo_.myPassword)) {
                HoxApp.getApp().disconnectAndLoginToServer();
            }
        }
    }

    // **** Implementation of HomeFragment.OnHomeFragmentListener ***
    @Override
    public void OnEditAccountViewClick() {
        Log.d(TAG, "OnEditAccountViewClick");
        openSettingsView();
    }
}
