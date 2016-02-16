package com.jambit.wearableworkshop;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MyWearableListenerService extends WearableListenerService {

    private static GoogleApiClient googleApiClient;
    private String LOG_TAG = "MyWearableListenerService";
    private static final long CONNECTION_TIME_OUT_MS = 100;
    private ExecutorService executorService;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.d(LOG_TAG, "Message received");
        if (messageEvent.getPath().equalsIgnoreCase("accumulator")) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            String noString = new String(messageEvent.getData());
            int no = Integer.parseInt(noString);
            noString = "" + (no + 1);
            reply(messageEvent.getPath(), messageEvent.getSourceNodeId(), noString.getBytes());
        } else {
            super.onMessageReceived(messageEvent);
        }
    }

    private void reply(final String path, final String nodeId, final byte[] bytes) {
        googleApiClient = new GoogleApiClient.Builder(getApplicationContext())
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle bundle) {
                        Log.d(LOG_TAG, "ConnectionCallback onConnected");
                        doReply(path, nodeId, bytes);
                    }

                    @Override
                    public void onConnectionSuspended(int i) {
                        Log.d(LOG_TAG, "ConnectionCallback onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(LOG_TAG, "ConnectionCallback onConnectionFailed");
                    }
                })
                .build();
        googleApiClient.connect();

    }

    private void doReply(final String path, final String nodeId, final byte[] bytes) {
        Log.v(LOG_TAG, "In reply()");
        Log.v(LOG_TAG, "Path: " + path);

        if (googleApiClient != null && !(googleApiClient.isConnected() || googleApiClient.isConnecting())) {
            googleApiClient.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        }

        this.executorService = Executors.newCachedThreadPool();
        this.executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.v(LOG_TAG, "Will sent now.");
                Wearable.MessageApi.sendMessage(googleApiClient, nodeId, path, bytes).await();
                Log.v(LOG_TAG, "Has recently sent.");

            }
        });
        Log.v(LOG_TAG, "Message sent.");

    }

}