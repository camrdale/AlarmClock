package org.camrdale.clock.shared.nearby;

import com.google.common.base.Objects;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WifiConnectionResponse that = (WifiConnectionResponse) o;
        return Objects.equal(networkSsid, that.networkSsid) &&
                Objects.equal(success, that.success) &&
                Objects.equal(failureReason, that.failureReason);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkSsid, success, failureReason);
    }

    @Override
    public String toString() {
        return "WifiConnectionResponse{" +
                "networkSsid='" + networkSsid + '\'' +
                ", success=" + success +
                ", failureReason='" + failureReason + '\'' +
                '}';
    }
}
