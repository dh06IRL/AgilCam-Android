package com.david.autodash.recordings;

import android.content.AsyncTaskLoader;
import android.content.Context;


import com.david.autodash.data.Recording;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by davidhodge on 11/16/14.
 */
public class RecordingsLoader extends AsyncTaskLoader<List<Recording>> {
    private File path;

    public RecordingsLoader(Context context, File path) {
        super(context);
        this.path = path;
    }

    @Override
    public List<Recording> loadInBackground() {
        File[] files = path.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return filename.endsWith("mp4");
            }
        });
        ArrayList<Recording> ret = new ArrayList<Recording>();
        for (File f : files) {
            ret.add(new Recording(f));
        }
        return ret;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }
}
