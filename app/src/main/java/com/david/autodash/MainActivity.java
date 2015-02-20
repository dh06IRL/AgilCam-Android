package com.david.autodash;

import android.app.ActionBar;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.display.VirtualDisplay;
import android.location.Location;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.david.autodash.data.RecordRequest;
import com.david.autodash.data.ServiceState;
import com.david.autodash.fragment.Camera2FragmentFront;
import com.david.autodash.fragment.Camera2VideoFragment;
import com.david.autodash.recordings.RecordingsActivity;
import com.david.autodash.utils.SpeedometerGauge;
import com.david.autodash.utils.TranslucentUrlTileProvider;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.melnykov.fab.FloatingActionButton;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class MainActivity extends Activity implements LocationListener, GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, CaptureService.Listener {

    RecordRequest request;
    MediaProjection projection;
    SharedPreferences sharedPreferences;
    SpeedometerGauge speedometerGauge;
    ActionBar actionBar;
    Context mContext;
    MapView mapView;
    GoogleMap mMap;
    LocationRequest mLocationRequest;
    LocationClient mLocationClient;
    private TileOverlay mainOverlay;
    RelativeLayout speedHolder;

    TextView elevationText;
    TextView gText;
    TextView headingText;
    TextView headingTextText;
    FloatingActionButton startRecord;
    FloatingActionButton showVideos;
    FloatingActionButton settings;
    TextView speedText;

    private CaptureService service;
    private ServiceState state;
    private MediaProjectionManager projectionManager;
    Surface surface;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd.HH_mm_ss", Locale.US);
    private ServiceConnection serviceConn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            service = ((CaptureService.LocalBinder)binder).getService();
            service.addListener(MainActivity.this);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            service = null;
            stateUpdated(null);
        }
    };

    // Milliseconds per second
    private static final int MILLISECONDS_PER_SECOND = 1000;
    // Update frequency in seconds
    public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
    // Update frequency in milliseconds
    private static final long UPDATE_INTERVAL =
            MILLISECONDS_PER_SECOND * UPDATE_INTERVAL_IN_SECONDS;
    // The fastest update frequency, in seconds
    private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
    // A fast frequency ceiling in milliseconds
    private static final long FASTEST_INTERVAL =
            MILLISECONDS_PER_SECOND * FASTEST_INTERVAL_IN_SECONDS;

    SensorManager mgr;

    static final float ALPHA = 0.01f;
    protected float[] accelVals;

    MediaRecorder mediaRecorder;

    private SensorEventListener listener=new SensorEventListener() {
        public void onSensorChanged(SensorEvent e) {
            if (e.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
                //Total acceleration will be sqrt(x^2+y^2+z^2)
                accelVals = lowPass(e.values, accelVals);
                double netForce = accelVals[0]*accelVals[0];    //X axis
                netForce += accelVals[1]*accelVals[1];    //Y axis
                netForce += (accelVals[2])*(accelVals[2]);    //Z axis (upwards)

                netForce = Math.sqrt(netForce) - SensorManager.GRAVITY_EARTH;    //Take the square root, minus gravity
                DecimalFormat df = new DecimalFormat("#.##");

                gText.setText(df.format(netForce) + "");
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // unused
        }
    };

    private Camera mFrontCamera;
    Camera2FragmentFront camera2FragmentFront;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);

        actionBar = getActionBar();
        actionBar.hide();

        speedometerGauge = (SpeedometerGauge) findViewById(R.id.speedometer);
        mapView = (MapView) findViewById(R.id.map);
        elevationText = (TextView) findViewById(R.id.elevation_text);
        gText = (TextView) findViewById(R.id.g_force_text);
        headingText = (TextView) findViewById(R.id.heading_text);
        headingTextText = (TextView) findViewById(R.id.heading_text_text);
        startRecord = (FloatingActionButton) findViewById(R.id.start_record);
        showVideos = (FloatingActionButton) findViewById(R.id.videos);
        settings = (FloatingActionButton) findViewById(R.id.settings);
        speedText = (TextView) findViewById(R.id.speed_text);
        speedHolder = (RelativeLayout) findViewById(R.id.speed_holder);

        if(sharedPreferences.getBoolean("speed", false) == true){
            speedText.setVisibility(View.VISIBLE);
            speedHolder.setVisibility(View.VISIBLE);
        }else{
            speedText.setVisibility(View.GONE);
            speedHolder.setVisibility(View.GONE);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        bindService(new Intent(this, CaptureService.class), serviceConn, BIND_AUTO_CREATE);

        speedometerGauge.setLabelConverter(new SpeedometerGauge.LabelConverter() {
            @Override
            public String getLabelFor(double progress, double maxProgress) {
                return String.valueOf((int) Math.round(progress));
            }
        });

        speedometerGauge.setVisibility(View.GONE);

        startRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                if (state.isRecording()) {
//                    service.stop();
//                } else {
                    startActivityForResult(projectionManager.createScreenCaptureIntent(), R.id.requestProjection);
//                }
            }
        });

        showVideos.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, RecordingsActivity.class));
            }
        });

        settings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(mContext, SettingsActivity.class));
            }
        });

        speedometerGauge.setMaxSpeed(200);
        speedometerGauge.setMajorTickStep(20);
        speedometerGauge.setMinorTicks(1);

        speedometerGauge.addColoredRange(80, 200, Color.RED);
        speedometerGauge.setLabelTextSize(20);

//        speedometerGauge.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                if (state.isRecording()) {
//                    service.stop();
//                    Toast.makeText(mContext, "Recording Ended", Toast.LENGTH_SHORT).show();
//                } else {
//                    startActivityForResult(projectionManager.createScreenCaptureIntent(), R.id.requestProjection);
//                }
//            }
//        });

        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, Camera2VideoFragment.newInstance())
                    .commit();
        }

//        mFrontCamera = getCameraInstance(1);
//        camera2FragmentFront = new Camera2FragmentFront(this, mFrontCamera);
//        FrameLayout frontPreview = (FrameLayout) findViewById(R.id.container_front);
//        frontPreview.addView(camera2FragmentFront);

        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(
                LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 5 seconds
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);


        mLocationClient = new LocationClient(this, this, this);
        try {
            MapsInitializer.initialize(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mapView.onCreate(savedInstanceState);
        setUpMapIfNeeded();

        mgr = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mgr.registerListener(listener,
                mgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_UI);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setUpMap2() {
        mMap.setIndoorEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setCompassEnabled(true);

        if(sharedPreferences.getBoolean("traffic", false) == true) {
            mMap.setTrafficEnabled(true);
        }

        mMap.setMyLocationEnabled(true);

        try {
            String mapType = sharedPreferences.getString("layer", null);
            if (mapType.contentEquals("normal")) {
                mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
            } else if (mapType.contentEquals("hybrid")) {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            } else if (mapType.contentEquals("sat")) {
                mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
            } else if (mapType.contentEquals("ter")) {
                mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
            } else {
                mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
            }
        } catch (Exception e) {
            mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        }

        if (mMap.getMyLocation() != null) {
            LatLng myLocation = new LatLng(mMap.getMyLocation().getLatitude(),
                    mMap.getMyLocation().getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation,
                    13));
        }

    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((MapView) findViewById(R.id.map)).getMap();
            if (mMap != null) {
                setUpMap2();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        // If the client is connected
        if (mLocationClient.isConnected()) {
            /*
             * Remove location updates for a listener.
             * The current Activity is the listener, so
             * the argument is "this".
             */
            mLocationClient.removeLocationUpdates(this);
        }
        /*
         * After disconnect() is called, the client is
         * considered "dead".
         */
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();

        if (state.isRecording()) {
            service.stop();
        }

        mgr.unregisterListener(listener);
        service.removeListener(this);
        unbindService(serviceConn);

//        if(mediaRecorder != null){
//            try{
//                mediaRecorder.stop();
//            }catch(RuntimeException stopException){
//                //handle cleanup here
//            }
//            mediaRecorder.release();
//        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    public void onLocationChanged(Location location) {

        elevationText.setText(Math.round(location.getAltitude()) + "");
        headingText.setText(Math.round(location.getBearing()) + "");
        headingTextText.setText(getWind(Math.round(location.getBearing())));
        //Move the camera to the user's location once it's available!
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 14));
        speedometerGauge.setSpeed(location.getSpeed(), true);
        if(location.getSpeed() != 0.0) {
            speedText.setText(Math.round(location.getSpeed() * 2.23694) + "");
        }else{
            speedText.setText(Math.round(location.getSpeed()) + "");
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        if (requestCode == R.id.requestProjection) {
            service.record(new RecordRequest.Builder(resultCode, data).durationMs(-1).build());
//            try {
//                DisplayMetrics metrics = getResources().getDisplayMetrics();
////            service.record(new RecordRequest.Builder(resultCode, data).durationMs(-1).build());
//                RecordRequest request = new RecordRequest.Builder(resultCode, data).durationMs(-1).build();
//                projection = request.getProjection(projectionManager);
//
//                final int flags = 0;
//                int width = 1280;
//                int height = 720;
//                MediaFormat format = MediaFormat.createVideoFormat("video/avc", width, height);
//                format.setInteger(MediaFormat.KEY_BIT_RATE, 8000000);
//                format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
//                format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
//                format.setFloat(MediaFormat.KEY_FRAME_RATE, 60.0f);
//                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
//
//                final MediaCodec avc = MediaCodec.createEncoderByType("video/avc");
//                avc.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
//                surface = avc.createInputSurface();
//                projection.createVirtualDisplay("auto", width, height, metrics.densityDpi, flags, surface, new VirtualDisplay.Callback() {
//                }, new Handler());
//
//                File stored = new File(getExternalFilesDir("recorded"), "recording." + dateFormat.format(new Date()) + ".mp4");
//                mediaRecorder = new MediaRecorder();
//                mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
//                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//                mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
//                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.DEFAULT);
//                mediaRecorder.setVideoSize(width, height);
//                mediaRecorder.setOutputFile(stored.getPath());
//                mediaRecorder.setPreviewDisplay(surface);
//
//                try {
//                    mediaRecorder.prepare();
//                } catch (IllegalStateException e) {
//                    // TODO Auto-generated catch block
//                    Log.e("error", "prepare " + e.toString());
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    Log.e("error", "prepare " + e.toString());
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//
//                try {
//                mediaRecorder.start();
//                } catch (IllegalStateException e) {
//                    // TODO Auto-generated catch block
//                    Log.e("error", "starts " + e.toString());
//                    e.printStackTrace();
//                }
//            }catch (IOException e){
//                Log.e("error", "record " +  e.toString());
//            }
        }
    }


    protected float[] lowPass( float[] input, float[] output ) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    public String getWind(double degree) {
        String direction;
        if (degree >= 348.75) {
            direction = "N";
        } else if (degree <= 11.25) {
            direction = "N";
        } else if (degree > 11.25 && degree <= 33.75) {
            direction = " NNE";
        } else if (degree > 33.75 && degree <= 56.25) {
            direction = "NE";
        } else if (degree > 56.25 && degree <= 78.75) {
            direction = "ENE";
        } else if (degree > 78.75 && degree <= 101.25) {
            direction = "E";
        } else if (degree > 101.25 && degree <= 123.75) {
            direction = "ESE";
        } else if (degree > 123.75 && degree <= 146.25) {
            direction = "SE";
        } else if (degree > 146.25 && degree <= 168.75) {
            direction = "SSE";
        } else if (degree > 168.75 && degree <= 191.25) {
            direction = "S";
        } else if (degree > 191.25 && degree <= 213.75) {
            direction = "SSW";
        } else if (degree > 213.75 && degree <= 236.25) {
            direction = "SW";
        } else if (degree > 236.25 && degree <= 258.75) {
            direction = "WSW";
        } else if (degree > 258.75 && degree <= 281.25) {
            direction = "W";
        } else if (degree > 281.25 && degree <= 303.75) {
            direction = "WNW";
        } else if (degree > 303.75 && degree <= 326.25) {
            direction = "NW";
        } else if (degree > 326.25 && degree <= 348.75) {
            direction = "NNW";
        } else {
            direction = "U/A";
        }
        return direction;
    }

    @Override
    public void stateUpdated(ServiceState state) {
        this.state = state;
        if (service == null || state == null) {
            return;
        }

        if (state.isRecording()) {
            startRecord.setVisibility(View.GONE);
            showVideos.setVisibility(View.GONE);
            settings.setVisibility(View.GONE);
        } else {
            startRecord.setVisibility(View.VISIBLE);
            showVideos.setVisibility(View.VISIBLE);
            settings.setVisibility(View.VISIBLE);
            Toast.makeText(mContext, "Recording Ended", Toast.LENGTH_SHORT).show();
        }
    }

    public static Camera getCameraInstance(int cameraId){
        Camera c = null;
        try {
            c = Camera.open(cameraId); // attempt to get a Camera instance
        }
        catch (Exception e){
            // Camera is not available (in use or does not exist)
            Log.e("app", "Camera " + cameraId + " not available! " + e.toString());
        }
        return c; // returns null if camera is unavailable
    }
}

