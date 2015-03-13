package com.playxiangqi.hoxchess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TablesActivity extends ActionBarActivity {

    private static final String TAG = "TablesActivity";
    
    private ListView tablesListView_;
    private List<TableInfo> tables_ = new ArrayList<TableInfo>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tables);
        
        Log.d(TAG, "onCreate:");
        
        tablesListView_ = (ListView)findViewById(R.id.list_tables);
        
        String listContent = getIntent().getExtras().getString("content");
        parseListContent(listContent);
        
        final ArrayList<TableInfo> listItems = new ArrayList<TableInfo>();
        for (TableInfo table : tables_) {
            listItems.add(table);
        }
        
        final StableArrayAdapter adapter = new StableArrayAdapter(this,
                android.R.layout.simple_list_item_1, listItems);
        tablesListView_.setAdapter(adapter);
        
        tablesListView_.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int itemPosition = position; // ListView Clicked item index
                TableInfo itemValue = (TableInfo) tablesListView_.getItemAtPosition(position);
                
                Log.d(TAG, "Position:" + itemPosition + " TabeId: " + itemValue.tableId
                        + ", ListItem: " + itemValue);
                
                // Return the table-ID.
                Intent result = new Intent();
                result.putExtra("tid", itemValue.tableId);
                setResult(Activity.RESULT_OK, result);
                finish();            }
        });
        
    }
    
    private void parseListContent(String listContent) {
        //Log.d(TAG, "Parse LIST 's content: [" + listContent + "]");
        
        final String[] entries = listContent.split("\n");
        for (String entry : entries) {
            Log.d(TAG, ">>> ..... entry [" + entry + "].");
            TableInfo tableInfo = new TableInfo(entry);
            tables_.add(tableInfo);
        }
        Log.d(TAG, ">>> Number of tables = " + tables_.size() + ".");
    }
    
    // =============================================================================
    private class StableArrayAdapter extends ArrayAdapter<TableInfo> {
        HashMap<TableInfo, Integer> mIdMap = new HashMap<TableInfo, Integer>();

        public StableArrayAdapter(Context context, int textViewResourceId,
                List<TableInfo> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            TableInfo item = getItem(position);
            Log.d(TAG, ">>> ..... (getItemId) position: " + position + " => [" + item + "]");
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
    
}
