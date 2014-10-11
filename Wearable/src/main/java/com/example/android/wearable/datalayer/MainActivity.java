/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wearable.datalayer;

import static com.example.android.wearable.datalayer.DataLayerListenerService.LOGD;

import android.app.Activity;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Shows events and photo from the Wearable APIs.
 */
public class MainActivity extends Activity implements SensorEventListener, ConnectionCallbacks,
        OnConnectionFailedListener {

    private static final String TAG = "MainActivity";

    private static final String HEARTRATE_PATH = "/heartrate";
    private static final String HEARTRATE_KEY = "heartrate";
    private GoogleApiClient mGoogleApiClient;
    private ListView mDataItemList;
    private TextView mIntroText;
//    private DataItemAdapter mDataItemListAdapter;
    private View mLayout;
    private Handler mHandler;
    private Sensor mHeartRateSensor;
    private float mHeartRate;
    private SensorManager mSensorManager;

    private ScheduledExecutorService mGeneratorExecutor;
    private ScheduledFuture<?> mDataItemGeneratorFuture;

//    private Object mLock;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
//        mHandler = new Handler();
//        mLock = new Object();
        setContentView(R.layout.main_activity);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mDataItemList = (ListView) findViewById(R.id.dataItem_list);
        mIntroText = (TextView) findViewById(R.id.intro);
        mLayout = findViewById(R.id.layout);

        mSensorManager = ((SensorManager)getSystemService(SENSOR_SERVICE));
        mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
//        mSensorManager.registerListener(this,mHeartRateSensor,3);

        mGeneratorExecutor = new ScheduledThreadPoolExecutor(1);
        // Stores data events received by the local broadcaster.
//        mDataItemListAdapter = new DataItemAdapter(this, android.R.layout.simple_list_item_1);
//        mDataItemList.setAdapter(mDataItemListAdapter);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
//        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
//                new DataItemGenerator(), 1, 5, TimeUnit.SECONDS);
//        mGoogleApiClient.connect();
//
//        int result = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
//        if( ConnectionResult.SUCCESS == result ){
//            mGoogleApiClient.connect();
//            mIntroText.setText("Connecting to Wear...");
//        }
//        else {
//            // Show appropriate dialog
//            Dialog d = GooglePlayServicesUtil.getErrorDialog(result, this, 0);
//            d.show();
//        }
    }

    public void onResume() {
        LOGD(TAG,"onResume");
        super.onResume();
        mSensorManager.registerListener(this,mHeartRateSensor,3);
        mDataItemGeneratorFuture = mGeneratorExecutor.scheduleWithFixedDelay(
                new DataItemGenerator(), 1, 3, TimeUnit.SECONDS);
    }
    @Override
    protected void onStart() {
        LOGD(TAG,"onStart");
        super.onStart();
        mGoogleApiClient.connect();
    }


    @Override
    protected void onPause() {
        LOGD(TAG,"onPause");
        super.onPause();
//        Wearable.DataApi.removeListener(mGoogleApiClient, this);
//        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
//        Wearable.NodeApi.removeListener(mGoogleApiClient, this);
        mGoogleApiClient.disconnect();
        mDataItemGeneratorFuture.cancel(true /* mayInterruptIfRunning */);
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onStop() {
        LOGD(TAG,"onStop");
        super.onStop();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LOGD(TAG, "onConnected(): Successfully connected to Google API client");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        LOGD(TAG, "onConnectionSuspended(): Connection to Google API client was suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.e(TAG, "onConnectionFailed(): Failed to connect, with result: " + result);
    }

//    private void generateEvent(final String title, final String text) {
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                mIntroText.setVisibility(View.INVISIBLE);
//                mDataItemListAdapter.add(new Event(title, text));
//            }
//        });
//    }

//    @Override
//    public void onDataChanged(DataEventBuffer dataEvents) {
//        LOGD(TAG, "onDataChanged(): " + dataEvents);
//
//        final List<DataEvent> events = FreezableUtils.freezeIterable(dataEvents);
//        dataEvents.close();
//        for (DataEvent event : events) {
//            if (event.getType() == DataEvent.TYPE_CHANGED) {
//                String path = event.getDataItem().getUri().getPath();
//                if (DataLayerListenerService.IMAGE_PATH.equals(path)) {
//                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
//                    Asset photo = dataMapItem.getDataMap()
//                            .getAsset(DataLayerListenerService.IMAGE_KEY);
//                    final Bitmap bitmap = loadBitmapFromAsset(mGoogleApiClient, photo);
//                    mHandler.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            Log.d(TAG, "Setting background image..");
//                            mLayout.setBackground(new BitmapDrawable(getResources(), bitmap));
//                        }
//                    });
//
//                } else if (DataLayerListenerService.COUNT_PATH.equals(path)) {
//                    LOGD(TAG, "Data Changed for COUNT_PATH");
//                    generateEvent("DataItem Changed", event.getDataItem().toString());
//                } else {
//                    LOGD(TAG, "Unrecognized path: " + path);
//                }
//
//            } else if (event.getType() == DataEvent.TYPE_DELETED) {
//                generateEvent("DataItem Deleted", event.getDataItem().toString());
//            } else {
//                generateEvent("Unknown data event type", "Type = " + event.getType());
//            }
//        }
//    }

//    @Override
//    public void onMessageReceived(MessageEvent event) {
//        LOGD(TAG, "onMessageReceived: " + event);
//
//        //generateEvent("Message", event.toString());
//    }
//
//    @Override
//    public void onPeerConnected(Node node) {
//        generateEvent("Node Connected", node.getId());
//    }
//
//    @Override
//    public void onPeerDisconnected(Node node) {
//        generateEvent("Node Disconnected", node.getId());
//    }


    @Override
    public void onSensorChanged(SensorEvent event) {
//        Log.d(TAG, "sensor event: " + event.accuracy + " = " + event.values[0]);
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
        Log.d(TAG, "accuracy changed: " + accuracy);
    }

    /**
     * Called by the system when the device configuration changes while your
     * activity is running.  Note that this will <em>only</em> be called if
     * you have selected configurations you would like to handle with the
     * {@link android.R.attr#configChanges} attribute in your manifest.  If
     * any configuration change occurs that is not selected to be reported
     * by that attribute, then instead of reporting it the system will stop
     * and restart the activity (to have it launched with the new
     * configuration).
     * <p/>
     * <p>At the time that this function has been called, your Resources
     * object will have been updated to return resource values matching the
     * new configuration.
     *
     * @param newConfig The new device configuration.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        LOGD(TAG,"onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
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
