package com.david.autodash.recordings;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;
import com.squareup.picasso.Downloader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Created by davidhodge on 11/16/14.
 */
public class ThumbDownloader implements Downloader {

    @Override
    public Response load(Uri uri, boolean localCacheOnly) throws IOException {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(new File(new URI(uri.toString().replace("http://", "file:///"))).getAbsolutePath());
            Bitmap bitmap = retriever.getFrameAtTime(8 * 1000 * 1000); // 15 secs in
            return new Response(bitmap, false, -1);
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
