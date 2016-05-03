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

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TablesFragment extends Fragment {

    private static final String TAG = "TablesFragment";
    private boolean DEBUG_LIFE_CYCLE = true;

    private OnFragmentInteractionListener listener_;

    private ListView tablesListView_;

    private TablesAdapter adapter_;

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
        void onTablesFragment_CreateView(TablesFragment fragment);
        void onTablesFragment_DestroyView(TablesFragment fragment);
        void onTableSelected(String tableId);
    }

    /**
     * Constructor
     */
    public TablesFragment() {
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "[CONSTRUCTOR]");
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment TablesFragment.
     */
    public static TablesFragment newInstance() {
        return new TablesFragment();
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
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onCreateView:");
        final View view = inflater.inflate(R.layout.fragment_tables, container, false);

        tablesListView_ = (ListView) view.findViewById(R.id.list_tables);

        adapter_ = new TablesAdapter(getActivity(), R.layout.listview_item_table);
        tablesListView_.setAdapter(adapter_);

        tablesListView_.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TableInfo itemValue = (TableInfo) tablesListView_.getItemAtPosition(position);
                Log.d(TAG, "Position:" + position + " TableId: " + itemValue.tableId
                        + ", ListItem: " + itemValue);
                listener_.onTableSelected(itemValue.tableId);
            }
        });

        listener_.onTablesFragment_CreateView(this);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onResume:");
    }

    @Override
    public void onPause() {
        super.onPause();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onPause:");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (DEBUG_LIFE_CYCLE) Log.v(TAG, "onDestroyView");
        listener_.onTablesFragment_DestroyView(this);
    }

    public void refreshView() {
        Log.d(TAG, "refreshView:...");
        adapter_.refreshTables();
    }

    /**
     * The custom adapter for our list view.
     */
    private static class TablesAdapter extends BaseAdapter {
        private final Activity activity_;
        private final int resourceId_;
        private final List<TableInfo> tables_ = new ArrayList<TableInfo>();

        public TablesAdapter(Activity context, int textViewResourceId) {
            activity_ = context;
            resourceId_ = textViewResourceId;
        }

        public void refreshTables() {
            tables_.clear();
            final List<TableInfo> latestTables = PlayerManager.getInstance().getTables();
            tables_.addAll(latestTables);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return tables_.size();
        }

        @Override
        public Object getItem(int position) {
            return tables_.get(position);
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
                holder.tableIdView = (TextView) convertView.findViewById(R.id.table_id);
                holder.playersInfoView = (TextView) convertView.findViewById(R.id.players_info);
                holder.gameInfoView = (TextView) convertView.findViewById(R.id.game_info);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            final TableInfo tableInfo = (TableInfo) getItem(position);
            holder.tableIdView.setText(tableInfo.tableId);
            holder.playersInfoView.setText(
                    String.format("%s vs. %s", tableInfo.getRedInfo(), tableInfo.getBlackInfo()));
            holder.gameInfoView.setText(tableInfo.itimes);

            return convertView;
        }

    }

    /**
     * The view holder for our custom adapter.
     */
    private static class ViewHolder {
        public TextView tableIdView;
        public TextView playersInfoView;
        public TextView gameInfoView;
    }

}
