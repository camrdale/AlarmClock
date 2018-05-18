package org.camrdale.clock.companion;

import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;

import com.google.common.base.CharMatcher;

import java.util.ArrayList;
import java.util.List;

public class WifiActivity extends AppCompatActivity {
    private static final String TAG = WifiActivity.class.getSimpleName();

    static final String WIFI_SSID = "WIFI_SSID";
    static final String WIFI_PASSWORD = "WIFI_PASSWORD";

    private AutoCompleteTextView ssid;
    private EditText password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        ssid = findViewById(R.id.wifiSsid);
        password = findViewById(R.id.wifiPassword);

        Button submit = findViewById(R.id.wifiSubmit);
        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra(WIFI_SSID, ssid.getText().toString());
                if (password.getText().toString().length() > 0) {
                    resultIntent.putExtra(WIFI_PASSWORD, password.getText().toString());
                }
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });

        Button cancel = findViewById(R.id.wifiCancel);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        WifiManager manager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        for (WifiConfiguration network : manager.getConfiguredNetworks()) {
            Log.i(TAG, "Wifi network: " + network.SSID + ", " + network.status + ", " + network.FQDN);
        }
        WifiInfo info = manager.getConnectionInfo();
        Log.i(TAG, "Wifi info: " + info.getSSID() + ", " + info.getIpAddress() + ", " + info.getLinkSpeed());
        Log.i(TAG, "Wifi state: " + manager.getWifiState());
        List<ScanResult> scan = manager.getScanResults();
        List<String> ssids = new ArrayList<>();
        for (ScanResult result : scan) {
            Log.i(TAG, "Scan result: " + result.SSID + ", " + result.capabilities);
            if (result.SSID != null && result.SSID.length() > 0) {
                ssids.add(result.SSID);
            }
        }

        if (info.getSSID() != null && info.getSSID().length() > 0) {
            String ssidOnly = CharMatcher.is('\"').trimFrom(info.getSSID());
            if (ssidOnly.length() > 0) {
                ssid.setText(ssidOnly);
            }
        }
        ssid.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, ssids));
    }
}
