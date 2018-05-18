package org.camrdale.clock.shared.nearby;

import com.google.gson.annotations.SerializedName;

public class WifiConnectionResponse {
    @SerializedName("network_ssid")
    private String networkSsid;

    private Boolean success;

    @SerializedName("failure_reason")
    private String failureReason;

    public WifiConnectionResponse(String networkSsid, boolean success, String failureReason) {
        this.networkSsid = networkSsid;
        this.success = success;
        this.failureReason = failureReason;
    }

    public String getNetworkSsid() {
        return networkSsid;
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getFailureReason() {
        return failureReason;
    }
}
