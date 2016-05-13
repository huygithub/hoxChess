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
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * A fragment with a Google +1 button.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnHomeFragmentListener} interface
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment
                    implements NetworkController.NetworkEventListener {

    private static final String TAG = "HomeFragment";
    private boolean DEBUG_LIFE_CYCLE = true;

    private static final int JOIN_TABLE_REQUEST = 1;

    private OnHomeFragmentListener listener_;

    private TextView loginTextView_;

    private SettingsActivity.SettingsInfo settingsInfo_;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HomeFragment.
     */
    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onCreateView");

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        loginTextView_ = (TextView) view.findViewById(R.id.home_login_text);

        View aiView = view.findViewById(R.id.home_new_ai_table_view);
        aiView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayAITable();
            }
        });

        View networkTableView = view.findViewById(R.id.home_new_network_table_view);
        networkTableView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayNetworkTable();
            }
        });

        View joinView = view.findViewById(R.id.home_join_existing_table_view);
        joinView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                displayTableList();
            }
        });

        View editView = view.findViewById(R.id.home_edit_account_view);
        editView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                listener_.OnEditAccountViewClick();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onResume");
        NetworkController.getInstance().addListener(this);
        // Refresh the state of the Login/Logout button each time the activity receives focus.
        refreshLoginState();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onPause");
        NetworkController.getInstance().removeListener(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnHomeFragmentListener) {
            listener_ = (OnHomeFragmentListener) context;
        } else {
            throw new RuntimeException(context.toString()
                   + " must implement OnHomeFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener_ = null;
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
    public interface OnHomeFragmentListener {
        void OnEditAccountViewClick();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult: requestCode:" + requestCode + ", resultCode:" + resultCode);
        if (resultCode != Activity.RESULT_OK) {
            return;
        }

        if (requestCode == JOIN_TABLE_REQUEST) {
            final String tableId = data.getStringExtra("tid");
            NetworkTableActivity.start(getActivity(), tableId);
        }
    }

    // **** Implementation of NetworkController.NetworkEventListener ****
    @Override
    public void onLoginSuccess() {
        Log.d(TAG, "onLoginSuccess:...");
        refreshLoginState();
    }

    @Override
    public void onLogout() {
        Log.d(TAG, "onLogout:...");
        refreshLoginState();
    }

    @Override
    public void onLoginFailure(int errorMessageResId) {
        Log.d(TAG, "onLoginFailure:...");
        //Snackbar.make(loginTextView_, errorMessageResId, Snackbar.LENGTH_LONG).show();
    }

    // *************

    private void refreshLoginState() {
        final boolean loginOK = NetworkController.getInstance().isLoginOK();
        Log.d(TAG, "refreshLoginState: loginOK = " + loginOK);
        CharSequence loginText;
        if (loginOK) {
            loginText = Html.fromHtml(getString(R.string.logged_in_as_player,
                    HoxApp.getApp().getMyPid(),
                    NetworkController.getInstance().getMyRating_()));
        } else {
            loginText = Html.fromHtml(getString(R.string.will_log_in_as_player,
                    HoxApp.getApp().getMyPid()));
        }
        loginTextView_.setText(loginText);
    }

    private void displayAITable() {
        Intent intent = new Intent(getActivity(), AITableActivity.class);
        startActivity(intent);
    }

    private void displayNetworkTable() {
        final String tableId = ""; // NOTE: Empty means "not yet assigned".
        NetworkTableActivity.start(getActivity(), tableId);
    }

    private void displayTableList() {
        PlayerManager.getInstance().clearTables(); // will get a new list
        NetworkController.getInstance().sendRequestForTableList();

        Intent intent = new Intent(getActivity(), TablesActivity.class);
        startActivityForResult(intent, JOIN_TABLE_REQUEST);
    }
}
