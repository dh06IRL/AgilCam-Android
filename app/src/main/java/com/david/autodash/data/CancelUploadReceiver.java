package com.david.autodash.data;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.david.autodash.CaptureService;

/**
 * Created by davidhodge on 11/14/14.
 */
public class CancelUploadReceiver extends BroadcastReceiver{

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent();
        service.setComponent(new ComponentName(context, CaptureService.class));
        context.stopService(service);
    }

}
