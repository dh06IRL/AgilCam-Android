package com.david.autodash;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.display.VirtualDisplay;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;

import com.david.autodash.data.CancelUploadReceiver;
import com.david.autodash.data.RecordRequest;
import com.david.autodash.data.ServiceState;
import com.david.autodash.recordings.MediaAudioEncoder;
import com.david.autodash.recordings.MediaEncoder;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.xml.transform.Source;

/**
 * Created by davidhodge on 11/14/14.
 */
public class CaptureService extends Service {
    Context mContext;
    MediaRecorder mediaRecorder;
    Surface surface;
    private final Set<Listener> listeners = new HashSet<Listener>();
    private ServiceState state = new ServiceState.Builder().build();
    private Handler uiHandler;

    private MediaProjectionManager projectionManager;
    private MediaProjection projection;
    private boolean running = false;
    int iReadResult = 0;
    public static final int SAMPLES_PER_FRAME = 1024; // AAC

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd.HH_mm_ss", Locale.US);

    public CaptureService() {
        uiHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        //noinspection ResourceType
        this.projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    public void record(RecordRequest request) {
        // notify about state change
        state = new ServiceState.Builder(state).recording(request).build();
        notifyStateChange();
        // start self
        startService(new Intent(this, getClass()));

        Intent deleteIntent = new Intent(this, CancelUploadReceiver.class);
        PendingIntent pendingIntentCancel = PendingIntent.getBroadcast(this, 0, deleteIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        // show ongoing notification
        Notification.Builder builder = new Notification.Builder(this);
        builder.setOngoing(true);
        builder.setSmallIcon(android.R.drawable.stat_notify_sync);
        builder.setContentTitle("Recording in progress");
        builder.addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop Recording", pendingIntentCancel);
//        startForeground(R.id.notification, builder.build());
        // kick off the recording
        projection = request.getProjection(projectionManager);
//        recordNew();
        record();
        // stop after duration passes
        if (request.getDurationMs() > 0) {
            uiHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stop();
                }
            }, request.getDurationMs());
        }
    }

    public void recordNew(){
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        final int flags = 0;

        try {
            int width = 1280;
            int height = 720;
            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 8000000);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setFloat(MediaFormat.KEY_FRAME_RATE, 60.0f);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

            final MediaCodec avc = MediaCodec.createEncoderByType("video/avc");
            avc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            surface = avc.createInputSurface();
            projection.createVirtualDisplay("auto", width, height, metrics.densityDpi, flags, surface, new VirtualDisplay.Callback() {
            }, uiHandler);
//            avc.start();

            File stored = new File(getExternalFilesDir("recorded"), "recording." + dateFormat.format(new Date()) + ".mp4");
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
            mediaRecorder.setOutputFile(stored.getPath());
            mediaRecorder.setPreviewDisplay(surface);

            try {
                mediaRecorder.prepare();
            } catch (IllegalStateException e) {
                // TODO Auto-generated catch block
                Log.e("error", e.toString());
                e.printStackTrace();
            } catch (IOException e) {
                Log.e("error", e.toString());
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            mediaRecorder.start();

        }catch (IOException e){
            Log.e("error", e.toString());
        }
    }

    private void record() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();

        int width = 1280;
        int height = 720;

        final int flags = 0;
        try {

            MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
            format.setInteger(MediaFormat.KEY_BIT_RATE, 8000000);
            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setFloat(MediaFormat.KEY_FRAME_RATE, 60.0f);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

            MediaFormat formatA = new MediaFormat();
            formatA.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
            formatA.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
            formatA.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100);
            formatA.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
            formatA.setInteger(MediaFormat.KEY_BIT_RATE, 64000);

            final MediaCodec avc = MediaCodec.createEncoderByType("video/avc");
            avc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            final Surface surface = avc.createInputSurface();
            avc.start();

            final File out = new File(getExternalCacheDir(), UUID.randomUUID().toString() + ".mp4");
            final MediaMuxer muxer = new MediaMuxer(out.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            final LocationService locationService = new LocationService(mContext);

            final int audioBufferSize = AudioRecord.getMinBufferSize(8000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            final AudioRecord mAudioRecorder = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, audioBufferSize*10);
            mAudioRecorder.startRecording();
//
            final MediaCodec mAudioEncoder = MediaCodec.createEncoderByType("audio/mp4a-latm");
            mAudioEncoder.configure(formatA, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//            mAudioEncoder.getInputBuffer(mAudioRecorder.getAudioSource());
            mAudioEncoder.start();


            running = true;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
//                    MediaCodec.BufferInfo infoA = new MediaCodec.BufferInfo();
                    int track = -1;
                    int audio = -1;
                    byte[] buffer200ms = new byte[8000 / 10];
                    while (running) {
                        int index = avc.dequeueOutputBuffer(info, 10000);
                        int indexA = mAudioRecorder.read(buffer200ms, 0, buffer200ms.length);
                        if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (track != -1) {
                                throw new RuntimeException("format changed twice");
                            }
                            track = muxer.addTrack(avc.getOutputFormat());
                            try {
                                muxer.setLocation((float) locationService.getLatitude(), (float) locationService.getLongitude());
                                locationService.stopSelf();
                            } catch (Exception e) {
                                Log.e("error", e.toString());
                            }
                            muxer.start();
                        } else if (index >= 0) {
                            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                // ignore codec config
                                info.size = 0;
                            }
                            if (track != -1) {
                                ByteBuffer out = avc.getOutputBuffer(index);
                                out.position(info.offset);
                                out.limit(info.offset + info.size);
                                muxer.writeSampleData(track, out, info);
                                avc.releaseOutputBuffer(index, false);
                            }

                        }

                        if (indexA == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                            if (audio != -1) {
                                throw new RuntimeException("format changed twice");
                            }
                            audio = muxer.addTrack(mAudioEncoder.getOutputFormat());
                        }else if(indexA >= 0){
                            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                                // ignore codec config
                                info.size = 0;
                            }
                            if(audio != -1){
                                ByteBuffer aOut =  mAudioEncoder.getOutputBuffer(indexA);
                                aOut.position(info.offset);
                                aOut.limit(info.offset + info.size);
                                muxer.writeSampleData(audio, aOut, info);
                                mAudioEncoder.releaseOutputBuffer(indexA, false);
                            }
                        }
                    }

                    File stored = new File(getExternalFilesDir("recorded"), "recording." + dateFormat.format(new Date()) + ".mp4");

                    mAudioEncoder.stop();
                    mAudioEncoder.release();

                    avc.stop();
                    avc.release();
                    projection.stop();
                    surface.release();
                    muxer.stop();
                    muxer.release();

                    // move output to media folder
//                    File stored = new File(getExternalFilesDir("recorded"), "recording." + dateFormat.format(new Date()) + ".mp4");
                    out.renameTo(stored);
                    // notify loader ?
                }
            }).start();

            projection.createVirtualDisplay("auto", width, height, metrics.densityDpi, flags, surface, new VirtualDisplay.Callback() {
            }, uiHandler);
        } catch (IOException e) {
            Log.e("error", "start " + e.toString());
            e.printStackTrace();
        }
    }

    public void stop() {
        // stop recording
        running = false;
        // notify state change
        state = new ServiceState.Builder(state).recording(null).build();
        notifyStateChange();
        // dismiss notification
        stopForeground(true);
        // stop self
        stopSelf();
    }

    public void addListener(Listener listener) {
        synchronized (listeners) {
            listener.stateUpdated(state);
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    public class LocalBinder extends Binder {
        public CaptureService getService() {
            return CaptureService.this;
        }
    }

    public interface Listener {
        void stateUpdated(ServiceState state);
    }

    private void notifyStateChange() {
        synchronized (listeners) {
            for (Listener l : listeners) {
                l.stateUpdated(state);
            }
        }
    }
}
