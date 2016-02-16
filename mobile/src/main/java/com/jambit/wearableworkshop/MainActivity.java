package com.jambit.wearableworkshop;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import com.google.android.gms.common.*;
import com.google.android.gms.common.api.*;
import com.google.android.gms.wearable.*;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements MessageApi.MessageListener {

    private static final String TAG = "MainActivity";

    private GoogleApiClient mApiClient;

    private ExecutorService executorService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button) findViewById(R.id.connect);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openConnection();
            }
        });


        Button button2 = (Button) findViewById(R.id.send);
        button2.setActivated(false);

    }

    void openConnection() {
        mApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    public void onConnected(Bundle bundle) {
                        Log.d(TAG, "ConnectionCallback onConnected");

                        Button button = (Button) findViewById(R.id.connect);
                        button.setText("Disconnect");
                        button.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                closeConnection();
                            }
                        });

                        Button button2 = (Button) findViewById(R.id.send);
                        button2.setActivated(true);
                        button2.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                sendMessageFromNonUiThread();
                            }
                        });
                    }

                    public void onConnectionSuspended(int i) {
                        Log.d(TAG, "ConnectionCallback onConnectionSuspended");
                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    public void onConnectionFailed(ConnectionResult connectionResult) {
                        Log.d(TAG, "ConnectionCallback onConnectionFailed");
                    }
                })
                .build();
        mApiClient.connect();

        Wearable.MessageApi.addListener(mApiClient, this);
    }

    public void closeConnection() {
        mApiClient.disconnect();
        Button button = (Button) findViewById(R.id.connect);
        button.setText("Connect");
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openConnection();
            }
        });

        Button button2 = (Button) findViewById(R.id.send);
        button2.setActivated(false);
        button2.setOnClickListener(null);
    }

    public void onMessageReceived(final MessageEvent messageEvent) {
        String received = new String(messageEvent.getData());
        Log.d(TAG, "onMessageReceived() " + received);
        Button button = (Button) findViewById(R.id.send);
        button.setText(received);
    }

    void sendMessageFromNonUiThread() {
        this.executorService = Executors.newCachedThreadPool();
        this.executorService.execute(new Runnable() {
            public void run() {
                sendMessage();
            }
        });
    }

    void sendMessage() {
        PendingResult< NodeApi.GetConnectedNodesResult > nodes = Wearable.NodeApi.getConnectedNodes(mApiClient);
        NodeApi.GetConnectedNodesResult nodesResult = nodes.await();
        List< Node > nodeList = nodesResult.getNodes();
        Button button = (Button) findViewById(R.id.send);
        for (Node node: nodeList) {
            CharSequence charSequence = button.getText();
            String text = charSequence.toString();
            byte[] bytes = text.getBytes();
            Wearable.MessageApi.sendMessage(mApiClient, node.getId(), "accumulator", bytes).await();
        }
        Log.d(TAG, "Message sent");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
