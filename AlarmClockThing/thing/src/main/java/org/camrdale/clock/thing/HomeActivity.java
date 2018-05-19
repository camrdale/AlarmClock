package org.camrdale.clock.thing;

import android.app.Activity;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.KeyEvent;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.gson.Gson;

import org.camrdale.clock.shared.nearby.WifiConnectionRequest;
import org.camrdale.clock.thing.peripherals.ButtonManager;
import org.camrdale.clock.thing.peripherals.DisplayManager;
import org.camrdale.clock.thing.peripherals.LedManager;
import org.camrdale.clock.thing.sounds.MediaManager;
import org.camrdale.clock.thing.state.StateManager;
import org.camrdale.clock.thing.web.WebManager;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Skeleton of an Android Things activity.
 */
public class HomeActivity extends Activity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    private static final String NEARBY_SERVICE_ID = "org.camrdale.clock.thing";

    @Inject DisplayManager displayManager;
    @Inject LedManager ledManager;
    @Inject ButtonManager buttonManager;
    @Inject StateManager stateManager;
    @Inject WebManager webManager;
    @Inject MediaManager mediaManager;

    private Handler cancelAdvertising;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        for (WifiConfiguration network : manager.getConfiguredNetworks()) {
            Log.i(TAG, "Wifi network: " + network.SSID + ", " + network.status + ", " + network.FQDN);
        }
        WifiInfo info = manager.getConnectionInfo();
        Log.i(TAG, "Wifi info: " + info.getSSID() + ", " + info.getIpAddress() + ", " + info.getLinkSpeed());
        Log.i(TAG, "Wifi state: " + manager.getWifiState());
        List<ScanResult> scan = manager.getScanResults();
        for (ScanResult result : scan) {
            Log.i(TAG, "Scan result: " + result.SSID + ", " + result.capabilities + ", " + result.operatorFriendlyName);
        }
    }

    private PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            String payloadString = new String(payload.asBytes(), StandardCharsets.UTF_8);
            Log.i(TAG, "Received nearby payload: " + payloadString);
            if (payloadString.startsWith(WifiConnectionRequest.class.getSimpleName())) {
                final WifiConnectionRequest wifiRequest = new Gson().fromJson(payloadString.substring(WifiConnectionRequest.class.getSimpleName().length() + 1), WifiConnectionRequest.class);
                if (wifiRequest.getNetworkSsid() != null && wifiRequest.getNetworkSsid().length() > 0) {
                        connectToWifi(wifiRequest.getNetworkSsid(), wifiRequest.getNetworkPassword());
                }
            }
        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
            Log.i(TAG, "Received nearby payload transfer update: " + payloadTransferUpdate.getBytesTransferred());
        }
    };

    private final ConnectionLifecycleCallback mConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        @NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                    Log.i(TAG, "Connection initiated: " + endpointId);
                    // Automatically accept the connection on both sides.
                    Nearby.getConnectionsClient(HomeActivity.this).acceptConnection(endpointId, mPayloadCallback);
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    Log.i(TAG, "Connection result: " + result.getStatus().getStatusCode());
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            stopAdvertising();
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

    private void startAdvertising() {
        Log.i(TAG, "Starting adverstising to nearby");
        cancelAdvertising = new Handler();
        cancelAdvertising.postDelayed(this::stopAdvertising, 60 * 1000);
        Nearby.getConnectionsClient(this).startAdvertising(
                "alarmclockthing",
                NEARBY_SERVICE_ID,
                mConnectionLifecycleCallback,
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
                .addOnSuccessListener(unusedResult -> Log.i(TAG, "Started adverstising to nearby"))
                .addOnFailureListener(e -> Log.i(TAG, "Failed adverstising to nearby: " + e));
    }

    private void stopAdvertising() {
        Log.i(TAG, "Stopping advertising to nearby");
        if (cancelAdvertising != null) {
            cancelAdvertising.removeCallbacksAndMessages(null);
            cancelAdvertising = null;
        }
        Nearby.getConnectionsClient(this).stopAdvertising();
    }

    private void connectToWifi(String networkSsid, String networkPassword) {
        WifiManager manager = (WifiManager) getSystemService(WIFI_SERVICE);
        for (WifiConfiguration network : manager.getConfiguredNetworks()) {
            if (network.SSID.contains(networkSsid)) {
                Log.i(TAG, "Disabling Wifi network: " + network.SSID + ", " + network.status + ", " + network.FQDN);
                boolean result = manager.disableNetwork(network.networkId);
                Log.i(TAG, "Disabled Wifi network: " + network.SSID + " with result " + result);
            }
        }

        Log.i(TAG, "Connecting to Wifi network: " + networkSsid);
        WifiConfiguration wifiConfiguration = new WifiConfiguration();
        wifiConfiguration.SSID = "\"" + networkSsid + "\"";
        if (networkPassword == null || networkPassword.isEmpty()) {
            wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        } else {
            wifiConfiguration.preSharedKey = "\"" + networkPassword + "\"";
        }

        int networkId = manager.addNetwork(wifiConfiguration);
        Log.i(TAG, "Added Wifi network: " + networkId);

        if (networkId != -1) {
            boolean result = manager.enableNetwork(networkId, true);
            Log.i(TAG, "Enabked Wifi network: " + result);

            result = manager.setWifiEnabled(true);
            Log.i(TAG, "Enabked Wifi: " + result);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            ledManager.setRedLed(true);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            ledManager.setGreenLed(true);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            ledManager.setBlueLed(true);
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            ledManager.setRedLed(false);
            //ConnectivityManager cManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            //NetworkInfo nInfo = cManager.getActiveNetworkInfo();
            //if (nInfo.getState() != NetworkInfo.State.CONNECTED) {
                startAdvertising();
            //} else {
            //    stateManager.registerKeyPress();
            //}
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            ledManager.setGreenLed(false);
            stateManager.sleepKeyPress();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            ledManager.setBlueLed(false);
            stateManager.snoozeKeyPress();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        webManager.cleanup();
        mediaManager.cleanup();
        stateManager.cleanup();
        displayManager.cleanup();
        ledManager.cleanup();
        buttonManager.cleanup();
    }
}
