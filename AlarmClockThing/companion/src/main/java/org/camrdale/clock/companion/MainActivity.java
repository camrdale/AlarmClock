package org.camrdale.clock.companion;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.gson.GsonBuilder;

import org.camrdale.clock.shared.nearby.WifiConnectionRequest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private static final int PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 23;
    private static final int CONNECT_ACTIVITY_RESULT = 87;
    private static final int WIFI_ACTIVITY_RESULT = 78;

    private String endpointId;
    private LinearLayoutManager mLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, ConnectActivity.class);
                startActivityForResult(intent, CONNECT_ACTIVITY_RESULT);
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            //if (ActivityCompat.shouldShowRequestPermissionRationale(this,
            //        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            //} else {
                // No explanation needed; request the permission
                Log.i(TAG, "Requsting location permission");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

                // PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            //}
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.i(TAG, "Requsting permission result: " + requestCode + ", " + Arrays.toString(grantResults));
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "Received activity result: " + requestCode + ", " + resultCode);
        if (requestCode == CONNECT_ACTIVITY_RESULT) {
            if (resultCode == RESULT_OK) {
                String endpointId = data.getStringExtra(ConnectActivity.NEARBY_ENDPOINT_ID);
                String endpointName = data.getStringExtra(ConnectActivity.NEARBY_ENDPOINT_NAME);
                Log.i(TAG, "Received connect activity result: " + endpointId + ", " + endpointName);
                Nearby.getConnectionsClient(MainActivity.this).acceptConnection(endpointId, mPayloadCallback);
                MainActivity.this.endpointId = endpointId;

                findViewById(R.id.no_devices_view).setVisibility(View.INVISIBLE);
                findViewById(R.id.deviceView).setVisibility(View.VISIBLE);

                ((TextView) findViewById(R.id.deviceNameView)).setText(endpointName);
                Button mWifiButton = findViewById(R.id.device_wifi_button);
                mWifiButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.i(TAG, "Launching wifi activity");
                        Intent intent = new Intent(MainActivity.this, WifiActivity.class);
                        startActivityForResult(intent, WIFI_ACTIVITY_RESULT);
                    }
                });
            } else {
                Log.w(TAG, "Connect activity cancelled");
            }
        } else if (requestCode == WIFI_ACTIVITY_RESULT) {
            if (resultCode == RESULT_OK) {
                String ssid = data.getStringExtra(WifiActivity.WIFI_SSID);
                String password = data.getStringExtra(WifiActivity.WIFI_PASSWORD);
                Log.i(TAG, "Received wifi activity result: " + ssid + ", " + password);
                connectToWifi(ssid, password);
            } else {
                Log.w(TAG, "Wifi activity cancelled");
            }
        }
    }

    private PayloadCallback mPayloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {
            Log.i(TAG, "Received nearby payload: " + new String(payload.asBytes(), StandardCharsets.UTF_8));

        }

        @Override
        public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {

        }
    };

    private void connectToWifi(String neworkSsid, String networkPassword) {
        if (endpointId != null) {
            WifiConnectionRequest request = new WifiConnectionRequest(neworkSsid, networkPassword);
            String json = new GsonBuilder().create().toJson(request);
            String payload = request.getClass().getSimpleName() + ":" + json;
            Nearby.getConnectionsClient(this).sendPayload(
                    endpointId, Payload.fromBytes(payload.getBytes()));
        } else {
            Log.w(TAG, "Nearby endpoint is not connected");
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ConnectionsClient client = Nearby.getConnectionsClient(this);
        if (endpointId != null) {
            Log.i(TAG, "Disconnecting nearby endpoint: " + endpointId);
            client.disconnectFromEndpoint(endpointId);
        }
        Log.i(TAG, "Disconnecting all nearby endpoints");
        client.stopAllEndpoints();
    }
}
