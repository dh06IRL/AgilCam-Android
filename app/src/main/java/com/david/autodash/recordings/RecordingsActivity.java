package com.david.autodash.recordings;

import android.app.ActionBar;
import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.MenuItem;

import com.david.autodash.R;
import com.david.autodash.data.Recording;

import java.io.File;
import java.util.List;

/**
 * Created by davidhodge on 11/16/14.
 */
public class RecordingsActivity extends Activity implements LoaderManager.LoaderCallbacks<List<Recording>>, RecordingsAdapter.Callback {

    private RecyclerView recycler;
    ActionBar actionBar;
    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.recordings_list);

        actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        recycler = (RecyclerView) findViewById(R.id.recordingsList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recycler.setLayoutManager(layoutManager);

        File path = getExternalFilesDir("recorded");
        Bundle args = new Bundle();
        args.putString("path", path.getAbsolutePath());
        getLoaderManager().initLoader(R.id.recordingsLoader, args, this);
    }

    @Override
    public Loader<List<Recording>> onCreateLoader(int id, Bundle args) {
        return new RecordingsLoader(mContext, new File(args.getString("path")));
    }

    @Override
    public void onLoadFinished(Loader<List<Recording>> loader, List<Recording> data) {
        recycler.swapAdapter(new RecordingsAdapter(data, this, mContext), true);
    }

    @Override
    public void onLoaderReset(Loader<List<Recording>> loader) {
        recycler.swapAdapter(null, true);
    }

    @Override
    public void onShare(Recording rec) {
        startActivity(rec.getShareIntent(mContext));
    }

    @Override
    public void onView(Recording rec) {
        startActivity(rec.getViewIntent(mContext));
    }

    @Override
    public void onDelete(Recording rec) {
        rec.delete();
        File path = mContext.getExternalFilesDir("recorded");
        Bundle args = new Bundle();
        args.putString("path", path.getAbsolutePath());
        getLoaderManager().restartLoader(R.id.recordingsLoader, args, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
