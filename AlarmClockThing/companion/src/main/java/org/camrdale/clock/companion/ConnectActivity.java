package org.camrdale.clock.companion;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

public class ConnectActivity extends AppCompatActivity {
    private static final String TAG = ConnectActivity.class.getSimpleName();

    private static final String NEARBY_SERVICE_ID = "org.camrdale.clock.thing";

    static final String NEARBY_ENDPOINT_ID = "NEARBY_ENDPOINT_ID";
    static final String NEARBY_ENDPOINT_NAME = "NEARBY_ENDPOINT_NAME";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        startDiscovery();
    }

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        @NonNull final String endpointId, final ConnectionInfo connectionInfo) {
                    Log.i(TAG, "Connection initiated");
                    new AlertDialog.Builder(ConnectActivity.this)
                            .setTitle("Accept connection to " + connectionInfo.getEndpointName())
                            .setMessage("Confirm the code " + connectionInfo.getAuthenticationToken().substring(0, 4) + " is also displayed on the other device")
                            .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // The user confirmed, so we can accept the connection.
                                    Intent resultIntent = new Intent();
                                    resultIntent.putExtra(NEARBY_ENDPOINT_ID, endpointId);
                                    resultIntent.putExtra(NEARBY_ENDPOINT_NAME, connectionInfo.getEndpointName());
                                    setResult(RESULT_OK, resultIntent);
                                    finish();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Log.i(TAG, "Rejecting connection to " + endpointId);
                                    // The user canceled, so we should reject the connection.
                                    Nearby.getConnectionsClient(ConnectActivity.this).rejectConnection(endpointId);
                                    setResult(RESULT_CANCELED);
                                    finish();
                                }
                            })
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .show();
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    Log.i(TAG, "Connection result: " + result.getStatus().getStatusCode());
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            break;
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    Log.i(TAG, "Connection disconnected");
                }
            };

    private final EndpointDiscoveryCallback mEndpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(
                        @NonNull String endpointId, @NonNull DiscoveredEndpointInfo discoveredEndpointInfo) {
                    // An endpoint was found!
                    Log.i(TAG, "Nearby endpoint discovered: " + endpointId);
                    Nearby.getConnectionsClient(ConnectActivity.this).requestConnection(
                            "alarmclockcompanion",
                            endpointId,
                            mConnectionLifecycleCallback)
                            .addOnSuccessListener(
                                    new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void unusedResult) {
                                            // We successfully requested a connection. Now both sides
                                            // must accept before the connection is established.
                                            Log.i(TAG, "Connection successfully requested.");
                                        }
                                    })
                            .addOnFailureListener(
                                    new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            // Nearby Connections failed to request the connection.
                                            Log.i(TAG, "Connection request failed.");
                                        }
                                    });
                }

                @Override
                public void onEndpointLost(@NonNull String endpointId) {
                    // A previously discovered endpoint has gone away.
                    Log.i(TAG, "Nearby endpoint disappeared.");
                }
            };

    private void startDiscovery() {
        Log.i(TAG, "Starting discovery of nearby.");
        Nearby.getConnectionsClient(this).startDiscovery(
                NEARBY_SERVICE_ID,
                mEndpointDiscoveryCallback,
                new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                // We're discovering!
                                Log.i(TAG, "Started discovering nearby");
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // We were unable to start discovering.
                                Log.i(TAG, "Failed to discover nearby");
                            }
                        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ConnectionsClient client = Nearby.getConnectionsClient(this);
        Log.i(TAG, "Stopping discovery of nearby.");
        client.stopDiscovery();
    }
}
