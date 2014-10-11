package com.example.android.wearable.datalayer;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.example.android.wearable.datalayer.DataLayerListenerService.LOGD;

/**
 * Created by Alpha Jin on 10/10/14.
 */
public class HeartRateMonitor extends Service implements SensorEventListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static String TAG = "HeartRateMonitor";
    private static final String HEARTRATE_PATH = "/heartrate";
    private static final String HEARTRATE_KEY = "heartrate";

    private GoogleApiClient mGoogleApiClient;
    private Sensor mHeartRateSensor;
    private float mHeartRate;
    private SensorManager mSensorManager;

    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * Called by the system when the service is first created.  Do not call this method directly.
     */
    @Override
    public void onCreate() {
        LOGD(TAG,"onCreate");
        super.onCreate();
        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        mSensorManager.registerListener(this,mHeartRateSensor,3);
        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new DataItemGenerator(), 1, 2, TimeUnit.SECONDS);
    }

    /**
     * Called by the system to notify a Service that it is no longer used and is being removed.  The
     * service should clean up any resources it holds (threads, registered
     * receivers, etc) at this point.  Upon return, there will be no more calls
     * in to this Service object and it is effectively dead.  Do not call this method directly.
     */
    @Override
    public void onDestroy() {
        LOGD(TAG,"onDestroy");
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);
        mGoogleApiClient.disconnect();
    }


    public static void LOGD(final String tag, String message) {
        if (true) {
            Log.d(tag, message);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    /**
     * Called when sensor values have changed.
     * <p>See {@link android.hardware.SensorManager SensorManager}
     * for details on possible sensor types.
     * <p>See also {@link android.hardware.SensorEvent SensorEvent}.
     * <p/>
     * <p><b>NOTE:</b> The application doesn't own the
     * {@link android.hardware.SensorEvent event}
     * object passed as a parameter and therefore cannot hold on to it.
     * The object may be part of an internal pool and may be reused by
     * the framework.
     *
     * @param event the {@link android.hardware.SensorEvent SensorEvent}.
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
        LOGD(TAG, "sensor event: " + event.accuracy + " = " + event.values[0]);
        mHeartRate = event.values[0];
    }

    /**
     * Called when the accuracy of the registered sensor has changed.
     * <p/>
     * <p>See the SENSOR_STATUS_* constants in
     * {@link android.hardware.SensorManager SensorManager} for details.
     *
     * @param sensor
     * @param accuracy The new accuracy of this sensor, one of
     *                 {@code SensorManager.SENSOR_STATUS_*}
     */
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /** Generates a DataItem based on an incrementing count. */
    private class DataItemGenerator implements Runnable {

        @Override
        public void run() {
            if (mHeartRate == 0.0) {
                return;
            }
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(HEARTRATE_PATH);
            putDataMapRequest.getDataMap().putFloat(HEARTRATE_KEY, mHeartRate);
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            LOGD(TAG, "Generating DataItem: " + request);
            if (!mGoogleApiClient.isConnected()) {
                LOGD(TAG, "return");
                return;
            }
            for (final Node node : nodes.getNodes()) {
                Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                        .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                            @Override
                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                if (!dataItemResult.getStatus().isSuccess()) {
                                    Log.e(TAG, "ERROR: failed to putDataItem, status code: "
                                            + dataItemResult.getStatus().getStatusCode());
                                } else {
                                    Log.d(TAG, "success send to " + node.getDisplayName());
                                }
                            }
                        });
            }
        }
    }
}
