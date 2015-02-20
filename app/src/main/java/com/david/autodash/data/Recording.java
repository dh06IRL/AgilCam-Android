package com.david.autodash.data;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;
/**
 * Created by davidhodge on 11/14/14.
 */
public class Recording {
    private final File file;

    public Recording(File f) {
        file = f;
    }

    public String getName() {
        return file.getName();
    }

    public Uri getPicassouri() {
        // escaped to go through Picasso's downloader
        return Uri.parse(Uri.fromFile(file).toString().replace("file:///", "http://"));
    }

    private Uri getContentUri(Context context) {
        return FileProvider.getUriForFile(context, "com.david.autodash.fileprovider", file);
    }

    public Intent getViewIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(getContentUri(context));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return intent;
    }

    public Intent getShareIntent(Context context) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_STREAM, getContentUri(context));
        intent.setType("video/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return intent;
    }

    public boolean delete() {
        return file.delete();
    }

}
