package org.camrdale.clock.thing.nearby;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.things.device.DeviceManager;
import com.google.common.base.CharMatcher;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.camrdale.clock.shared.nearby.FactoryResetRequest;
import org.camrdale.clock.shared.nearby.FactoryResetResponse;
import org.camrdale.clock.shared.nearby.RebootRequest;
import org.camrdale.clock.shared.nearby.RebootResponse;
import org.camrdale.clock.shared.nearby.RegisterRequest;
import org.camrdale.clock.shared.nearby.RegisterResponse;
import org.camrdale.clock.shared.nearby.SignInRequest;
import org.camrdale.clock.shared.nearby.SignInResponse;
import org.camrdale.clock.shared.nearby.StatusBroadcast;
import org.camrdale.clock.shared.nearby.WifiConnectionRequest;
import org.camrdale.clock.shared.nearby.WifiConnectionResponse;
import org.camrdale.clock.thing.alarm.AlarmStorage;
import org.camrdale.clock.thing.peripherals.DisplayManager;
import org.camrdale.clock.thing.state.StateManager;

import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class NearbyManager extends ConnectionLifecycleCallback {
    private static final String TAG = NearbyManager.class.getSimpleName();

    private static final String NEARBY_SERVICE_ID = "org.camrdale.clock.thing";

    private Handler cancelAdvertising;
    private FirebaseAuth mAuth;
    private String endpointId;
    private CountDownTimer countDownTimer;

    private AlarmStorage alarmStorage;
    private StateManager stateManager;
    private DisplayManager displayManager;
    private final Context context;

    @Inject NearbyManager(
            AlarmStorage alarmStorage,
            StateManager stateManager,
            DisplayManager displayManager,
            Context context) {
        super();
        this.alarmStorage = alarmStorage;
        this.stateManager = stateManager;
        this.displayManager = displayManager;
        this.context = context;
        mAuth = FirebaseAuth.getInstance();

        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
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

    public void pairing() {
        startAdvertising();
    }

    private PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            String payloadString = new String(payload.asBytes(), StandardCharsets.UTF_8);
            Log.i(TAG, "Received nearby payload: " + payloadString);
            if (payloadString.startsWith(WifiConnectionRequest.class.getSimpleName() + ":")) {
                WifiConnectionRequest wifiRequest = new Gson().fromJson(payloadString.substring(WifiConnectionRequest.class.getSimpleName().length() + 1), WifiConnectionRequest.class);
                if (wifiRequest.getNetworkSsid() != null && wifiRequest.getNetworkSsid().length() > 0) {
                    connectToWifi(wifiRequest.getNetworkSsid(), wifiRequest.getNetworkPassword());
                }
            } else if (payloadString.startsWith(SignInRequest.class.getSimpleName() + ":")) {
                SignInRequest signInRequest = new Gson().fromJson(payloadString.substring(SignInRequest.class.getSimpleName().length() + 1), SignInRequest.class);
                if (signInRequest.getAuthenticationToken() != null && signInRequest.getAuthenticationToken().length() > 0) {
                    signIn(signInRequest.getAuthenticationToken());
                }
            } else if (payloadString.startsWith(RegisterRequest.class.getSimpleName() + ":")) {
                RegisterRequest rebootRequest = new Gson().fromJson(payloadString.substring(RegisterRequest.class.getSimpleName().length() + 1), RegisterRequest.class);
                register();
            } else if (payloadString.startsWith(RebootRequest.class.getSimpleName() + ":")) {
                RebootRequest rebootRequest = new Gson().fromJson(payloadString.substring(RebootRequest.class.getSimpleName().length() + 1), RebootRequest.class);
                reboot();
            } else if (payloadString.startsWith(FactoryResetRequest.class.getSimpleName() + ":")) {
                FactoryResetRequest resetRequest = new Gson().fromJson(payloadString.substring(FactoryResetRequest.class.getSimpleName().length() + 1), FactoryResetRequest.class);
                factoryReset();
            }

        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
        }
    };

    @Override
    public void onConnectionInitiated(
            @NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
        Log.i(TAG, "Connection initiated: " + endpointId);

        Log.i(TAG, "Displaying authentication token: " + connectionInfo.getAuthenticationToken());
        displayManager.setVerificationCodeDisplayMode(connectionInfo.getAuthenticationToken());

        // Automatically accept the connection on this side.
        Nearby.getConnectionsClient(context).acceptConnection(endpointId, mPayloadCallback);
        this.endpointId = endpointId;
    }

    @Override
    public void onConnectionResult(@NonNull String endpointId, @NonNull ConnectionResolution result) {
        Log.i(TAG, "Connection result: " + result.getStatus().getStatusCode());
        switch (result.getStatus().getStatusCode()) {
            case ConnectionsStatusCodes.STATUS_OK:
                // We're connected! Can now start sending and receiving data.
                stopAdvertising();

                countDownTimer = new CountDownTimer(Long.MAX_VALUE, 1000) {
                    // This is called after every 1 sec interval.
                    public void onTick(long millisUntilFinished) {
                        sendStatusBroadcast();
                    }

                    public void onFinish() {
                        start();
                    }
                }.start();
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
        Log.i(TAG, "Connection disconnected: " + endpointId);
        if (countDownTimer != null) {
            try {
                countDownTimer.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (this.endpointId != null) {
            this.endpointId = null;
        }
    }

    private void startAdvertising() {
        Log.i(TAG, "Starting adverstising to nearby");
        cancelAdvertising = new Handler();
        cancelAdvertising.postDelayed(this::stopAdvertising, 60 * 1000);
        Nearby.getConnectionsClient(context).startAdvertising(
                "alarmclockthing",
                NEARBY_SERVICE_ID,
                this,
                new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build())
                .addOnSuccessListener(unusedResult -> Log.i(TAG, "Started adverstising to nearby"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed adverstising to nearby", e));
    }

    private void stopAdvertising() {
        Log.i(TAG, "Stopping advertising to nearby");
        displayManager.setDisplayMode(DisplayManager.DisplayMode.HOUR_MINUTE);
        if (cancelAdvertising != null) {
            cancelAdvertising.removeCallbacksAndMessages(null);
            cancelAdvertising = null;
        }
        Nearby.getConnectionsClient(context).stopAdvertising();
    }

    private void sendPayload(Object response) {
        String json = new GsonBuilder().create().toJson(response);
        String payload = response.getClass().getSimpleName() + ":" + json;
        if (endpointId != null) {
            Nearby.getConnectionsClient(context).sendPayload(
                    endpointId, Payload.fromBytes(payload.getBytes()));
        } else {
            Log.w(TAG, "Nearby endpoint is not connected to send: " + payload);
        }
    }

    private void sendStatusBroadcast() {
        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = null;
        if (manager.isWifiEnabled()) {
            info = manager.getConnectionInfo();
        }
        boolean wifiConnected = info != null && info.getNetworkId() != -1;

        FirebaseUser user = mAuth.getCurrentUser();

        sendPayload(new StatusBroadcast(
                wifiConnected,
                wifiConnected ? CharMatcher.is('\"').trimFrom(info.getSSID()) : null,
                user != null,
                user != null ? user.getEmail() : null,
                user != null ? user.getDisplayName() : null,
                SystemClock.uptimeMillis(),
                alarmStorage.getWebKey().isPresent()));
    }

    private void connectToWifi(String networkSsid, String networkPassword) {
        try {
            WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
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

                sendPayload(new WifiConnectionResponse(networkSsid, true, null));
            } else {
                sendPayload(new WifiConnectionResponse(networkSsid, false, "Failed to add WiFi network"));
            }
        } catch (Exception e) {
            sendPayload(new WifiConnectionResponse(networkSsid, false, e.toString()));
        }
    }

    private void signIn(String authenticationToken) {
        try {
            Log.i(TAG, "Getting Google auth credential for token: " + authenticationToken);
            AuthCredential credential = GoogleAuthProvider.getCredential(authenticationToken, null);
            Log.i(TAG, "Signing in with credential: " + credential);
            mAuth.signInWithCredential(credential).addOnSuccessListener(authResult -> {
                Log.i(TAG, "Successful signin for token: " + authenticationToken);
                FirebaseUser user = authResult.getUser();
                Log.i(TAG, "Authenticated as user: " + user.getUid() + ", " + user.getDisplayName() + ", " + user.getEmail());
                sendPayload(new SignInResponse(true, null));
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Failed signin for token " + authenticationToken, e);
                sendPayload(new SignInResponse(false, e.toString()));
            });
        } catch (Exception e) {
            sendPayload(new SignInResponse(false, e.toString()));
        }
    }

    private void register() {
        try {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                Log.i(TAG, "Authenticated as user: " + user.getUid() + ", " + user.getDisplayName() + ", " + user.getEmail());
                user.getIdToken(false).addOnSuccessListener(token -> {
                    Log.i(TAG, "Successful getting of token: " + token.getClaims());
                    stateManager.register(
                            token.getToken(),
                            t -> sendPayload(new RegisterResponse(true, null)),
                            e -> sendPayload(new RegisterResponse(false, e.toString())));
                }).addOnFailureListener(e -> {
                    Log.e(TAG, "Failed getting of token for user " + user.getEmail(), e);
                    sendPayload(new RegisterResponse(false, e.toString()));
                });
            }
        } catch (Exception e) {
            sendPayload(new RegisterResponse(false, e.toString()));
        }
    }

    private void reboot() {
        try {
            DeviceManager deviceManager = DeviceManager.getInstance();
            deviceManager.reboot();
        } catch (Exception e) {
            sendPayload(new RebootResponse(false, e.toString()));
        }
    }

    private void factoryReset() {
        try {
            DeviceManager deviceManager = DeviceManager.getInstance();
            deviceManager.factoryReset(false);
        } catch (Exception e) {
            sendPayload(new FactoryResetResponse(false, e.toString()));
        }
    }

    public void cleanup() {
        stopAdvertising();
        if (countDownTimer != null) {
            try {
                countDownTimer.cancel();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (endpointId != null) {
            Log.i(TAG, "Disconnecting nearby endpoint: " + endpointId);
            ConnectionsClient client = Nearby.getConnectionsClient(context);
            client.disconnectFromEndpoint(endpointId);
            endpointId = null;
        }
    }
}
