package org.camrdale.clock.shared.nearby;

import com.google.gson.annotations.SerializedName;

public class WifiConnectionRequest {
    @SerializedName("network_ssid")
    private String networkSsid;

    @SerializedName("network_password")
    private String networkPassword;

    public WifiConnectionRequest(String networkSsid, String networkPassword) {
        this.networkSsid = networkSsid;
        this.networkPassword = networkPassword;
    }

    public String getNetworkSsid() {
        return networkSsid;
    }

    public String getNetworkPassword() {
        return networkPassword;
    }
}
