package com.david.autodash;

import android.app.Application;

import com.david.autodash.recordings.ThumbDownloader;
import com.squareup.picasso.Picasso;

/**
 * Created by davidhodge on 11/6/14.
 */
public class MainApp extends Application {

    public static Picasso picasso;

    @Override
    public void onCreate() {
        super.onCreate();
        Picasso.Builder builder = new Picasso.Builder(this);
        builder.indicatorsEnabled(BuildConfig.DEBUG);
        builder.downloader(new ThumbDownloader());
        picasso = builder.build();
    }
}
